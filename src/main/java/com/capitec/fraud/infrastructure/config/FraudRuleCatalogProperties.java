package com.capitec.fraud.infrastructure.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.rule-catalog")
public record FraudRuleCatalogProperties(
        @NotNull Boolean enabledByDefault,
        @NotNull Boolean seedOnStartup)
{
}
