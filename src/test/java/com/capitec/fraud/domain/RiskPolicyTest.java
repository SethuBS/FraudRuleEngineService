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
                RiskLevel.MEDIUM,
                RiskLevel.HIGH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mediumRiskMinimum");
    }
}
