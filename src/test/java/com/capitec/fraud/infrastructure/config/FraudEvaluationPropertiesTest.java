package com.capitec.fraud.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.infrastructure.config.FraudEvaluationProperties.DecisionThresholds;
import com.capitec.fraud.infrastructure.config.FraudEvaluationProperties.RiskThresholds;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FraudEvaluationPropertiesTest
{

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(FraudEvaluationConfiguration.class);

    @Test
    void mapsConfiguredThresholdsToDomainRiskPolicy()
    {
        var properties = new FraudEvaluationProperties(
                new RiskThresholds(10, 20, 30),
                new DecisionThresholds(RiskLevel.HIGH, RiskLevel.CRITICAL));

        var riskPolicy = properties.toRiskPolicy();

        assertThat(riskPolicy.riskLevelFor(RiskScore.of(25))).isEqualTo(RiskLevel.HIGH);
        assertThat(riskPolicy.decisionFor(RiskScore.of(25))).isEqualTo(FraudDecision.REVIEW);
        assertThat(riskPolicy.decisionFor(RiskScore.of(35))).isEqualTo(FraudDecision.FLAGGED);
    }

    @Test
    void bindsRiskPolicyFromApplicationConfiguration()
    {
        contextRunner.run(context ->
        {
            assertThat(context).hasSingleBean(RiskPolicy.class);

            var riskPolicy = context.getBean(RiskPolicy.class);

            assertThat(riskPolicy.riskLevelFor(RiskScore.of(55))).isEqualTo(RiskLevel.HIGH);
            assertThat(riskPolicy.decisionFor(RiskScore.of(55))).isEqualTo(FraudDecision.FLAGGED);
        });
    }
}
