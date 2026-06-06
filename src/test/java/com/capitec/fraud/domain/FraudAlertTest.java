package com.capitec.fraud.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FraudAlertTest
{

    private static final Instant CREATED_AT = Instant.parse("2026-06-07T08:03:00Z");
    private static final RiskPolicy RISK_POLICY = RiskPolicyFixtures.baselinePolicy();

    @Test
    void rejectsNonAlertableDecisions()
    {
        assertThatThrownBy(() -> new FraudAlert(
                UUID.randomUUID(),
                TransactionTest.sampleTransaction(),
                FraudDecision.REVIEW,
                RiskScore.of(25),
                RiskLevel.MEDIUM,
                RISK_POLICY,
                List.of(RuleEvaluationResult.matched("VELOCITY", "Velocity", 25, "Matched", CREATED_AT)),
                CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alertable");
    }

    @Test
    void rejectsUnmatchedRulesInAlertMatchedRules()
    {
        assertThatThrownBy(() -> FraudAlert.open(
                UUID.randomUUID(),
                TransactionTest.sampleTransaction(),
                RiskScore.of(55),
                List.of(RuleEvaluationResult.notMatched("VELOCITY", "Velocity", 25, "Not matched", CREATED_AT)),
                RISK_POLICY,
                CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unmatched");
    }
}
