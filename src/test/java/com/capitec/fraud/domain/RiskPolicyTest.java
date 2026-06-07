package com.capitec.fraud.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RiskPolicyTest
{

    @Test
    void derivesRiskLevelAndDecisionFromConfiguredThresholds()
    {
        var policy = new RiskPolicy(
                RiskScore.of(10),
                RiskScore.of(20),
                RiskScore.of(30),
                RiskScore.of(100),
                RiskLevel.HIGH,
                RiskLevel.CRITICAL);

        assertThat(policy.riskLevelFor(RiskScore.of(21))).isEqualTo(RiskLevel.HIGH);
        assertThat(policy.decisionFor(RiskScore.of(21))).isEqualTo(FraudDecision.REVIEW);
        assertThat(policy.decisionFor(RiskScore.of(31))).isEqualTo(FraudDecision.FLAGGED);
    }

    @Test
    void rejectsInvalidThresholdOrdering()
    {
        assertThatThrownBy(() -> new RiskPolicy(
                RiskScore.of(50),
                RiskScore.of(25),
                RiskScore.of(75),
                RiskScore.of(100),
                RiskLevel.MEDIUM,
                RiskLevel.HIGH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mediumRiskMinimum");
    }

    @Test
    void capsAggregatedMatchedRuleScoresAtConfiguredMaximum()
    {
        var policy = RiskPolicyFixtures.baselinePolicy();

        var riskScore = policy.aggregateScore(java.util.List.of(
                RuleEvaluationResult.matched(
                        "HIGH_VALUE_TRANSACTION",
                        "High Value Transaction",
                        75,
                        "Amount exceeded threshold",
                        java.time.Instant.parse("2026-06-07T08:02:00Z")),
                RuleEvaluationResult.matched(
                        "VELOCITY_TRANSACTION",
                        "Velocity Transaction",
                        75,
                        "Velocity threshold met",
                        java.time.Instant.parse("2026-06-07T08:02:00Z"))));

        assertThat(riskScore).isEqualTo(RiskScore.of(100));
    }
}
