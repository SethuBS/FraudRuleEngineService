package com.capitec.fraud.domain;

import java.time.Instant;

public record RuleEvaluationResult(
        String ruleCode,
        String ruleName,
        boolean matched,
        RiskScore scoreContribution,
        String explanation,
        Instant evaluatedAt)
{

    public RuleEvaluationResult
    {
        ruleCode = DomainValidation.requireUppercaseCode(ruleCode, "ruleCode");
        ruleName = DomainValidation.requireNonBlank(ruleName, "ruleName");
        scoreContribution = DomainValidation.requirePresent(scoreContribution, "scoreContribution");
        explanation = DomainValidation.requireNonBlank(explanation, "explanation");
        evaluatedAt = DomainValidation.requireInstant(evaluatedAt, "evaluatedAt");
    }

    public static RuleEvaluationResult matched(
            String ruleCode,
            String ruleName,
            int scoreContribution,
            String explanation,
            Instant evaluatedAt)
    {
        return new RuleEvaluationResult(ruleCode, ruleName, true, RiskScore.of(scoreContribution), explanation, evaluatedAt);
    }

    public static RuleEvaluationResult notMatched(
            String ruleCode,
            String ruleName,
            int scoreContribution,
            String explanation,
            Instant evaluatedAt)
    {
        return new RuleEvaluationResult(ruleCode, ruleName, false, RiskScore.of(scoreContribution), explanation, evaluatedAt);
    }

    public RiskScore effectiveScore()
    {
        return matched ? scoreContribution : RiskScore.ZERO;
    }
}
