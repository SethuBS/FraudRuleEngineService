package com.capitec.fraud.api;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured API error response.")
public record ApiErrorResponse(
        @Schema(description = "Stable error code.")
        String code,
        @Schema(description = "Safe human-readable error message.")
        String message,
        @Schema(description = "Request correlation id for support and log lookup.")
        String correlationId,
        @Schema(description = "Field-level validation errors.")
        List<FieldValidationError> fieldErrors)
{

    public ApiErrorResponse
    {
        fieldErrors = List.copyOf(fieldErrors);
    }

    public static ApiErrorResponse of(
            String code,
            String message,
            String correlationId)
    {
        return new ApiErrorResponse(code, message, correlationId, List.of());
    }

    public static ApiErrorResponse of(
            String code,
            String message,
            String correlationId,
            List<FieldValidationError> fieldErrors)
    {
        return new ApiErrorResponse(code, message, correlationId, fieldErrors);
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
