package com.capitec.fraud.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RiskScoreTest
{

    @ParameterizedTest
    @CsvSource({
        "0,LOW,APPROVED,false,false",
        "24,LOW,APPROVED,false,false",
        "25,MEDIUM,REVIEW,true,false",
        "49,MEDIUM,REVIEW,true,false",
        "50,HIGH,FLAGGED,true,true",
        "74,HIGH,FLAGGED,true,true",
        "75,CRITICAL,FLAGGED,true,true"
    })
    void derivesRiskLevelAndDecisionConsistently(
            int score,
            RiskLevel expectedRiskLevel,
            FraudDecision expectedDecision,
            boolean expectedManualReview,
            boolean expectedAlert)
    {
        var policy = RiskPolicyFixtures.baselinePolicy();
        var riskScore = RiskScore.of(score);

        assertThat(riskScore.riskLevel(policy)).isEqualTo(expectedRiskLevel);
        assertThat(FraudDecision.from(riskScore, policy)).isEqualTo(expectedDecision);
        assertThat(expectedDecision.requiresManualReview()).isEqualTo(expectedManualReview);
        assertThat(expectedDecision.requiresAlert()).isEqualTo(expectedAlert);
    }

    @ParameterizedTest
    @CsvSource({"-1", "-100"})
    void rejectsNegativeRiskScores(int score)
    {
        assertThatThrownBy(() -> RiskScore.of(score))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskScore");
    }
}
