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

    public static ValidationErrorResponse resourceNotFound(String message)
    {
        return new ValidationErrorResponse("RESOURCE_NOT_FOUND", message, List.of());
    }

    public record FieldValidationError(String field, String message)
    {
    }
}
