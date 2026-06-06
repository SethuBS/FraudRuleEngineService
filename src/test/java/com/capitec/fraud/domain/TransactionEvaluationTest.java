package com.capitec.fraud.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TransactionEvaluationTest
{

    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T08:02:00Z");
    private static final RiskPolicy RISK_POLICY = RiskPolicyFixtures.baselinePolicy();

    @Test
    void sumsMatchedRulesOnlyAndDerivesFlaggedDecision()
    {
        var evaluation = TransactionEvaluation.from(
                TransactionTest.sampleTransaction(),
                List.of(
                        RuleEvaluationResult.matched("HIGH_AMOUNT", "High Amount", 35, "Amount exceeded threshold", EVALUATED_AT),
                        RuleEvaluationResult.notMatched("VELOCITY", "Velocity", 25, "Below velocity threshold", EVALUATED_AT),
                        RuleEvaluationResult.matched("NEW_MERCHANT", "New Merchant", 20, "New merchant high value", EVALUATED_AT)),
                RISK_POLICY,
                EVALUATED_AT);

        assertThat(evaluation.riskScore()).isEqualTo(RiskScore.of(55));
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(evaluation.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(evaluation.requiresAlert()).isTrue();
        assertThat(evaluation.matchedRules()).hasSize(2);

        var alertId = UUID.fromString("b3ed20ca-bac6-45dc-a37e-d61001ee37ab");
        assertThat(evaluation.toFraudAlert(alertId))
                .hasValueSatisfying(alert ->
                {
                    assertThat(alert.alertId()).isEqualTo(alertId);
                    assertThat(alert.decision()).isEqualTo(FraudDecision.FLAGGED);
                    assertThat(alert.riskScore()).isEqualTo(RiskScore.of(55));
                    assertThat(alert.riskPolicy()).isEqualTo(RISK_POLICY);
                    assertThat(alert.matchedRules()).hasSize(2);
                });
    }

    @Test
    void derivesReviewDecisionWithoutFraudAlertForMediumRisk()
    {
        var evaluation = TransactionEvaluation.from(
                TransactionTest.sampleTransaction(),
                List.of(RuleEvaluationResult.matched("VELOCITY", "Velocity", 25, "Velocity threshold met", EVALUATED_AT)),
                RISK_POLICY,
                EVALUATED_AT);

        assertThat(evaluation.riskScore()).isEqualTo(RiskScore.of(25));
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(evaluation.decision()).isEqualTo(FraudDecision.REVIEW);
        assertThat(evaluation.requiresAlert()).isFalse();
        assertThat(evaluation.toFraudAlert(UUID.randomUUID())).isEmpty();
    }

    @Test
    void rejectsInconsistentManuallyConstructedEvaluation()
    {
        assertThatThrownBy(() -> new TransactionEvaluation(
                TransactionTest.sampleTransaction(),
                FraudDecision.APPROVED,
                RiskScore.of(55),
                RiskLevel.HIGH,
                RISK_POLICY,
                List.of(RuleEvaluationResult.matched("HIGH_AMOUNT", "High Amount", 55, "Matched", EVALUATED_AT)),
                EVALUATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decision");
    }
}
