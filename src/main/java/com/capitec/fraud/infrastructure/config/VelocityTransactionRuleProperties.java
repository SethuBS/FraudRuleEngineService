package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.rules.velocity-transaction")
public record VelocityTransactionRuleProperties(
        @NotNull @Positive Integer transactionCountThreshold,
        @NotNull @Positive Integer timeWindowMinutes,
        @NotNull @PositiveOrZero Integer defaultScore,
        @NotNull RiskLevel severity)
{

    Duration toTimeWindow()
    {
        return Duration.ofMinutes(timeWindowMinutes);
    }

    RiskScore toRiskScore()
    {
        return RiskScore.of(defaultScore);
    }
}
