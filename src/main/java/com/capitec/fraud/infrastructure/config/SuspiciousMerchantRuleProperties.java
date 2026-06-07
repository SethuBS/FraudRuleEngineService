package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.rules.suspicious-merchant")
public record SuspiciousMerchantRuleProperties(
        @NotNull List<@NotBlank String> merchantIds,
        @NotNull List<@NotBlank String> merchantNameFragments,
        @NotNull @PositiveOrZero Integer defaultScore,
        @NotNull RiskLevel severity)
{

    RiskScore toRiskScore()
    {
        return RiskScore.of(defaultScore);
    }
}
