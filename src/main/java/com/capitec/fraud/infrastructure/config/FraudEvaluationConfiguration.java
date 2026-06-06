package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.domain.RiskPolicy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FraudEvaluationProperties.class)
public class FraudEvaluationConfiguration
{

    @Bean
    RiskPolicy riskPolicy(FraudEvaluationProperties properties)
    {
        return properties.toRiskPolicy();
    }
}
