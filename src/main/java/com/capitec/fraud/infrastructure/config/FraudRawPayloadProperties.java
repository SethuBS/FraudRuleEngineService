package com.capitec.fraud.infrastructure.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.raw-payload")
public record FraudRawPayloadProperties(
        @NotNull Duration retentionDuration,
        @NotBlank String redactedValue,
        @NotEmpty List<@NotBlank String> sensitiveFieldNames)
{
}
