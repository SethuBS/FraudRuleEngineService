package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.rules.high-value-transaction")
public record HighValueTransactionRuleProperties(
        @NotNull @Positive BigDecimal thresholdAmount,
        @NotNull @PositiveOrZero Integer defaultScore,
        @NotNull RiskLevel severity)
{

    RiskScore toRiskScore()
    {
        return RiskScore.of(defaultScore);
    }
}
