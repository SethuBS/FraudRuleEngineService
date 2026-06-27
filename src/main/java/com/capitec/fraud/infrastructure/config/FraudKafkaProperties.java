package com.capitec.fraud.infrastructure.config;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.kafka")
public record FraudKafkaProperties(
        boolean enabled,
        @NotBlank String transactionEventsTopic,
        @NotBlank String deadLetterTopic,
        @NotBlank String consumerGroup,
        @NotBlank String expectedEventType,
        @NotBlank String supportedSchemaVersion,
        @Valid @NotNull Headers headers,
        @Valid @NotNull Retry retry,
        @Valid @NotNull Topics topics)
{

    public record Headers(
            @NotBlank String correlationId,
            @NotBlank String eventId,
            @NotBlank String eventType,
            @NotBlank String schemaVersion)
    {
    }

    public record Retry(
            @Min(1) int maxAttempts,
            @NotNull Duration backoff)
    {
        public Retry
        {
            if (backoff == null || backoff.isZero() || backoff.isNegative())
            {
                throw new IllegalArgumentException("fraud.kafka.retry.backoff must be positive");
            }
        }
    }

    public record Topics(
            @Min(1) int partitions,
            @Min(1) int replicationFactor)
    {
    }
}
