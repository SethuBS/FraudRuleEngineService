package com.capitec.fraud.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.observability")
public record FraudObservabilityProperties(
        @NotBlank String correlationIdMdcKey,
        @Min(1) int correlationIdMaxLength)
{
}
