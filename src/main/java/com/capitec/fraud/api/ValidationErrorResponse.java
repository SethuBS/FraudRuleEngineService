package com.capitec.fraud.api;

import java.util.List;

public record ValidationErrorResponse(
        String code,
        String message,
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

    public record FieldValidationError(String field, String message)
    {
    }
}
