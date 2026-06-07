package com.capitec.fraud.api.dto;

import com.capitec.fraud.domain.RuleEvaluationResult;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Matched fraud rule evidence for an evaluation.")
public record MatchedRuleResponse(
        @Schema(description = "Stable fraud rule code.")
        String ruleCode,
        @Schema(description = "Human-readable fraud rule name.")
        String ruleName,
        @Schema(description = "Score contributed by the matched rule.")
        int scoreContribution,
        @Schema(description = "Human-readable reason explaining the rule outcome.")
        String explanation,
        @Schema(description = "Timestamp when the rule was evaluated.")
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
