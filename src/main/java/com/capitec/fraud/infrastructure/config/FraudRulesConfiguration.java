package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.rules.HighValueTransactionRule;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HighValueTransactionRuleProperties.class)
public class FraudRulesConfiguration
{

    @Bean
    HighValueTransactionRule highValueTransactionRule(HighValueTransactionRuleProperties properties)
    {
        return new HighValueTransactionRule(
                properties.thresholdAmount(),
                properties.toRiskScore(),
                properties.severity());
    }
}
