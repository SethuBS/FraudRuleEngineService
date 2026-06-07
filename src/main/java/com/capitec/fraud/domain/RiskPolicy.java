package com.capitec.fraud.domain;

import java.util.Collection;

public record RiskPolicy(
        RiskScore mediumRiskMinimum,
        RiskScore highRiskMinimum,
        RiskScore criticalRiskMinimum,
        RiskScore maximumRiskScore,
        RiskLevel reviewMinimumRiskLevel,
        RiskLevel flaggedMinimumRiskLevel)
{

    public RiskPolicy
    {
        mediumRiskMinimum = DomainValidation.requirePresent(mediumRiskMinimum, "mediumRiskMinimum");
        highRiskMinimum = DomainValidation.requirePresent(highRiskMinimum, "highRiskMinimum");
        criticalRiskMinimum = DomainValidation.requirePresent(criticalRiskMinimum, "criticalRiskMinimum");
        maximumRiskScore = DomainValidation.requirePresent(maximumRiskScore, "maximumRiskScore");
        reviewMinimumRiskLevel = DomainValidation.requirePresent(reviewMinimumRiskLevel, "reviewMinimumRiskLevel");
        flaggedMinimumRiskLevel = DomainValidation.requirePresent(flaggedMinimumRiskLevel, "flaggedMinimumRiskLevel");

        if (mediumRiskMinimum.compareTo(highRiskMinimum) >= 0)
        {
            throw new IllegalArgumentException("mediumRiskMinimum must be less than highRiskMinimum");
        }
        if (highRiskMinimum.compareTo(criticalRiskMinimum) >= 0)
        {
            throw new IllegalArgumentException("highRiskMinimum must be less than criticalRiskMinimum");
        }
        if (criticalRiskMinimum.compareTo(maximumRiskScore) > 0)
        {
            throw new IllegalArgumentException("criticalRiskMinimum must be less than or equal to maximumRiskScore");
        }
        if (flaggedMinimumRiskLevel.compareTo(reviewMinimumRiskLevel) < 0)
        {
            throw new IllegalArgumentException("flaggedMinimumRiskLevel must be greater than or equal to reviewMinimumRiskLevel");
        }
    }

    public RiskScore aggregateScore(Collection<RuleEvaluationResult> ruleResults)
    {
        return DomainValidation.requirePresent(ruleResults, "ruleResults")
                .stream()
                .map(RuleEvaluationResult::effectiveScore)
                .reduce(RiskScore.ZERO, (currentScore, nextScore) -> cappedScore(currentScore.plus(nextScore)));
    }

    public RiskScore cappedScore(RiskScore riskScore)
    {
        var score = DomainValidation.requirePresent(riskScore, "riskScore");
        if (score.compareTo(maximumRiskScore) > 0)
        {
            return maximumRiskScore;
        }

        return score;
    }

    public RiskLevel riskLevelFor(RiskScore riskScore)
    {
        var score = cappedScore(riskScore);
        if (score.compareTo(criticalRiskMinimum) >= 0)
        {
            return RiskLevel.CRITICAL;
        }
        if (score.compareTo(highRiskMinimum) >= 0)
        {
            return RiskLevel.HIGH;
        }
        if (score.compareTo(mediumRiskMinimum) >= 0)
        {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    public FraudDecision decisionFor(RiskScore riskScore)
    {
        var riskLevel = riskLevelFor(riskScore);
        if (riskLevel.isAtLeast(flaggedMinimumRiskLevel))
        {
            return FraudDecision.FLAGGED;
        }
        if (riskLevel.isAtLeast(reviewMinimumRiskLevel))
        {
            return FraudDecision.REVIEW;
        }

        return FraudDecision.APPROVED;
    }
}
