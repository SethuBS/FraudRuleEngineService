package com.capitec.fraud.api;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured API error response.")
public record ValidationErrorResponse(
        @Schema(description = "Stable error code.")
        String code,
        @Schema(description = "Human-readable error message.")
        String message,
        @Schema(description = "Field-level validation errors.")
        List<FieldValidationError> fieldErrors)
{

    public ValidationErrorResponse
    {
        fieldErrors = List.copyOf(fieldErrors);
    }

    public static ValidationErrorResponse invalidRequest(List<FieldValidationError> fieldErrors)
    {
        return new ValidationErrorResponse("VALIDATION_FAILED", "Request validation failed", fieldErrors);
    }

    public static ValidationErrorResponse invalidRequest(String message)
    {
        return new ValidationErrorResponse("VALIDATION_FAILED", message, List.of());
    }

    public static ValidationErrorResponse resourceNotFound(String message)
    {
        return new ValidationErrorResponse("RESOURCE_NOT_FOUND", message, List.of());
    }

    @Schema(description = "Field-level validation error.")
    public record FieldValidationError(
            @Schema(description = "Request field name.")
            String field,
            @Schema(description = "Validation message.")
            String message)
    {
    }
}
