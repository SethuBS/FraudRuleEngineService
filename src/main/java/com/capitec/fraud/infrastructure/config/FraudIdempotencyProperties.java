package com.capitec.fraud.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.idempotency")
public record FraudIdempotencyProperties(@NotBlank String completedStatus)
{
}
