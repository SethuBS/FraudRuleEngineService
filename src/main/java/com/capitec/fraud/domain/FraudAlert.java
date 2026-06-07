package com.capitec.fraud.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudAlert(
        UUID alertId,
        Transaction transaction,
        FraudDecision decision,
        RiskScore riskScore,
        RiskLevel riskLevel,
        RiskPolicy riskPolicy,
        List<RuleEvaluationResult> matchedRules,
        Instant createdAt)
{

    public FraudAlert
    {
        alertId = DomainValidation.requirePresent(alertId, "alertId");
        transaction = DomainValidation.requirePresent(transaction, "transaction");
        decision = DomainValidation.requirePresent(decision, "decision");
        riskScore = DomainValidation.requirePresent(riskScore, "riskScore");
        riskLevel = DomainValidation.requirePresent(riskLevel, "riskLevel");
        riskPolicy = DomainValidation.requirePresent(riskPolicy, "riskPolicy");
        createdAt = DomainValidation.requireInstant(createdAt, "createdAt");
        matchedRules = List.copyOf(DomainValidation.requirePresent(matchedRules, "matchedRules"));

        if (!decision.requiresAlert())
        {
            throw new IllegalArgumentException("FraudAlert can only be opened for alertable decisions");
        }
        if (riskScore.riskLevel(riskPolicy) != riskLevel)
        {
            throw new IllegalArgumentException("riskLevel must match riskScore");
        }
        if (FraudDecision.from(riskScore, riskPolicy) != decision)
        {
            throw new IllegalArgumentException("decision must match riskScore");
        }
        if (!riskPolicy.cappedScore(riskScore).equals(riskScore))
        {
            throw new IllegalArgumentException("riskScore must not exceed maximumRiskScore");
        }
        if (matchedRules.stream().anyMatch(rule -> !rule.matched()))
        {
            throw new IllegalArgumentException("FraudAlert matchedRules cannot contain unmatched rule results");
        }
    }

    public static FraudAlert open(
            UUID alertId,
            Transaction transaction,
            RiskScore riskScore,
            List<RuleEvaluationResult> matchedRules,
            RiskPolicy riskPolicy,
            Instant createdAt)
    {
        var alertPolicy = DomainValidation.requirePresent(riskPolicy, "riskPolicy");
        return new FraudAlert(
                alertId,
                transaction,
                FraudDecision.from(riskScore, alertPolicy),
                riskScore,
                riskScore.riskLevel(alertPolicy),
                alertPolicy,
                matchedRules,
                createdAt);
    }
}
