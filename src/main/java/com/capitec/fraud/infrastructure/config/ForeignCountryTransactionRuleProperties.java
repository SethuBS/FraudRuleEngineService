package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.rules.foreign-country-transaction")
public record ForeignCountryTransactionRuleProperties(
        @NotBlank String expectedCountry,
        @NotNull @PositiveOrZero Integer defaultScore,
        @NotNull RiskLevel severity)
{

    String normalizedExpectedCountry()
    {
        return expectedCountry.strip().toUpperCase(Locale.ROOT);
    }

    RiskScore toRiskScore()
    {
        return RiskScore.of(defaultScore);
    }
}
