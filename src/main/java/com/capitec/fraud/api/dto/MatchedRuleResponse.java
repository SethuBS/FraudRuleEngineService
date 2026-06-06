package com.capitec.fraud.api.dto;

import com.capitec.fraud.domain.RuleEvaluationResult;

import java.time.Instant;

public record MatchedRuleResponse(
        String ruleCode,
        String ruleName,
        int scoreContribution,
        String explanation,
        Instant evaluatedAt)
{

    public static MatchedRuleResponse from(RuleEvaluationResult result)
    {
        return new MatchedRuleResponse(
                result.ruleCode(),
                result.ruleName(),
                result.scoreContribution().value(),
                result.explanation(),
                result.evaluatedAt());
    }
}
