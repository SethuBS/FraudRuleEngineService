package com.capitec.fraud.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.openapi")
public record FraudOpenApiProperties(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String version,
        @NotBlank String bearerSecuritySchemeName,
        @NotBlank String bearerScheme,
        @NotBlank String bearerFormat,
        @Valid @NotNull Examples examples)
{

    public record Examples(
            @NotBlank String highRiskTransaction,
            @NotBlank String lowRiskTransaction,
            @NotBlank String duplicateEvent,
            @NotBlank String highRiskResponse,
            @NotBlank String lowRiskResponse,
            @NotBlank String alertListResponse,
            @NotBlank String alertResponse,
            @NotBlank String validationErrorResponse,
            @NotBlank String notFoundResponse)
    {
    }
}
