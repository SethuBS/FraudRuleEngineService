package com.capitec.fraud.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record TransactionEvaluation(
        Transaction transaction,
        FraudDecision decision,
        RiskScore riskScore,
        RiskLevel riskLevel,
        RiskPolicy riskPolicy,
        List<RuleEvaluationResult> ruleResults,
        Instant evaluatedAt)
{

    public TransactionEvaluation
    {
        transaction = DomainValidation.requirePresent(transaction, "transaction");
        decision = DomainValidation.requirePresent(decision, "decision");
        riskScore = DomainValidation.requirePresent(riskScore, "riskScore");
        riskLevel = DomainValidation.requirePresent(riskLevel, "riskLevel");
        riskPolicy = DomainValidation.requirePresent(riskPolicy, "riskPolicy");
        ruleResults = List.copyOf(DomainValidation.requirePresent(ruleResults, "ruleResults"));
        evaluatedAt = DomainValidation.requireInstant(evaluatedAt, "evaluatedAt");

        if (riskScore.riskLevel(riskPolicy) != riskLevel)
        {
            throw new IllegalArgumentException("riskLevel must match riskScore");
        }
        if (FraudDecision.from(riskScore, riskPolicy) != decision)
        {
            throw new IllegalArgumentException("decision must match riskScore");
        }
    }

    public static TransactionEvaluation from(
            Transaction transaction,
            Collection<RuleEvaluationResult> ruleResults,
            RiskPolicy riskPolicy,
            Instant evaluatedAt)
    {
        var immutableResults = List.copyOf(DomainValidation.requirePresent(ruleResults, "ruleResults"));
        var evaluationPolicy = DomainValidation.requirePresent(riskPolicy, "riskPolicy");
        var riskScore = immutableResults.stream()
                .map(RuleEvaluationResult::effectiveScore)
                .reduce(RiskScore.ZERO, RiskScore::plus);

        return new TransactionEvaluation(
                transaction,
                FraudDecision.from(riskScore, evaluationPolicy),
                riskScore,
                riskScore.riskLevel(evaluationPolicy),
                evaluationPolicy,
                immutableResults,
                evaluatedAt);
    }

    public List<RuleEvaluationResult> matchedRules()
    {
        return ruleResults.stream()
                .filter(RuleEvaluationResult::matched)
                .toList();
    }

    public boolean requiresAlert()
    {
        return decision.requiresAlert();
    }

    public Optional<FraudAlert> toFraudAlert(UUID alertId)
    {
        if (!requiresAlert())
        {
            return Optional.empty();
        }

        return Optional.of(FraudAlert.open(alertId, transaction, riskScore, matchedRules(), riskPolicy, evaluatedAt));
    }
}
