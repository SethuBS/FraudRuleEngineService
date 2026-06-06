package com.capitec.fraud.domain;

public record RiskPolicy(
        RiskScore mediumRiskMinimum,
        RiskScore highRiskMinimum,
        RiskScore criticalRiskMinimum,
        RiskLevel reviewMinimumRiskLevel,
        RiskLevel flaggedMinimumRiskLevel)
{

    public RiskPolicy
    {
        mediumRiskMinimum = DomainValidation.requirePresent(mediumRiskMinimum, "mediumRiskMinimum");
        highRiskMinimum = DomainValidation.requirePresent(highRiskMinimum, "highRiskMinimum");
        criticalRiskMinimum = DomainValidation.requirePresent(criticalRiskMinimum, "criticalRiskMinimum");
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
        if (flaggedMinimumRiskLevel.compareTo(reviewMinimumRiskLevel) < 0)
        {
            throw new IllegalArgumentException("flaggedMinimumRiskLevel must be greater than or equal to reviewMinimumRiskLevel");
        }
    }

    public RiskLevel riskLevelFor(RiskScore riskScore)
    {
        var score = DomainValidation.requirePresent(riskScore, "riskScore");
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
