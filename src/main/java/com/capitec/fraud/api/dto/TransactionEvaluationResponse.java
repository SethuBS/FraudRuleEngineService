package com.capitec.fraud.api.dto;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.TransactionEvaluation;

import java.time.Instant;
import java.util.List;

public record TransactionEvaluationResponse(
        String eventId,
        String transactionId,
        String customerId,
        String accountId,
        FraudDecision decision,
        int riskScore,
        RiskLevel riskLevel,
        List<MatchedRuleResponse> matchedRules,
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
