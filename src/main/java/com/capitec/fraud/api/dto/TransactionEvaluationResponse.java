package com.capitec.fraud.api.dto;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.TransactionEvaluation;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fraud evaluation decision returned after evaluating a transaction.")
public record TransactionEvaluationResponse(
        @Schema(description = "Unique event id used for idempotency.")
        String eventId,
        @Schema(description = "Business transaction id.")
        String transactionId,
        @Schema(description = "Customer identifier.")
        String customerId,
        @Schema(description = "Account identifier.")
        String accountId,
        @Schema(description = "Final fraud decision.")
        FraudDecision decision,
        @Schema(description = "Final capped risk score.")
        int riskScore,
        @Schema(description = "Final risk level.")
        RiskLevel riskLevel,
        @Schema(description = "Rules that matched and contributed to the decision.")
        List<MatchedRuleResponse> matchedRules,
        @Schema(description = "Timestamp when the evaluation completed.")
        Instant evaluatedAt)
{

    public TransactionEvaluationResponse
    {
        matchedRules = List.copyOf(matchedRules);
    }

    public static TransactionEvaluationResponse from(TransactionEvaluation evaluation)
    {
        var transaction = evaluation.transaction();
        var matchedRuleResponses = evaluation.matchedRules()
                .stream()
                .map(MatchedRuleResponse::from)
                .toList();

        return new TransactionEvaluationResponse(
                transaction.eventId(),
                transaction.transactionId(),
                transaction.customerId(),
                transaction.accountId(),
                evaluation.decision(),
                evaluation.riskScore().value(),
                evaluation.riskLevel(),
                matchedRuleResponses,
                evaluation.evaluatedAt());
    }
}
