package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.rules.ForeignCountryTransactionRule;
import com.capitec.fraud.rules.HighValueTransactionRule;
import com.capitec.fraud.rules.VelocityTransactionRule;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    ForeignCountryTransactionRuleProperties.class,
    HighValueTransactionRuleProperties.class,
    VelocityTransactionRuleProperties.class
})
public class FraudRulesConfiguration
{

    @Bean
    ForeignCountryTransactionRule foreignCountryTransactionRule(ForeignCountryTransactionRuleProperties properties)
    {
        return new ForeignCountryTransactionRule(
                properties.normalizedExpectedCountry(),
                properties.toRiskScore(),
                properties.severity());
    }

    @Bean
    HighValueTransactionRule highValueTransactionRule(HighValueTransactionRuleProperties properties)
    {
        return new HighValueTransactionRule(
                properties.thresholdAmount(),
                properties.toRiskScore(),
                properties.severity());
    }

    @Bean
    VelocityTransactionRule velocityTransactionRule(VelocityTransactionRuleProperties properties)
    {
        return new VelocityTransactionRule(
                properties.transactionCountThreshold(),
                properties.toTimeWindow(),
                properties.toRiskScore(),
                properties.severity());
    }
}
