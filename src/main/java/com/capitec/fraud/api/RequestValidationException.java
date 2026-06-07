package com.capitec.fraud.api;

import com.capitec.fraud.api.ValidationErrorResponse.FieldValidationError;

import java.util.List;

public class RequestValidationException extends RuntimeException
{

    private final List<FieldValidationError> fieldErrors;

    public RequestValidationException(List<FieldValidationError> fieldErrors)
    {
        super("Request validation failed");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldValidationError> getFieldErrors()
    {
        return fieldErrors;
    }
}
