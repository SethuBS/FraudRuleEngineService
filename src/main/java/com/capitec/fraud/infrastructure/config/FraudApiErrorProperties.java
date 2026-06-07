package com.capitec.fraud.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.api.errors")
public record FraudApiErrorProperties(
        @NotBlank String correlationIdHeader,
        @NotBlank String validationFailedMessage,
        @NotBlank String malformedRequestMessage,
        @NotBlank String invalidParameterMessage,
        @NotBlank String duplicateEvaluationMessage,
        @NotBlank String authenticationRequiredMessage,
        @NotBlank String forbiddenMessage,
        @NotBlank String internalServerErrorMessage,
        @NotBlank String resourceNotFoundMessage)
{
}
