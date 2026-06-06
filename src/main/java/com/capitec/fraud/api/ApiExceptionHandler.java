package com.capitec.fraud.api;

import static java.util.Comparator.comparing;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.capitec.fraud.api.ValidationErrorResponse.FieldValidationError;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler
{

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException exception)
    {
        var fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(comparing(FieldError::getField))
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(BAD_REQUEST).body(ValidationErrorResponse.invalidRequest(fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ValidationErrorResponse> handleIllegalArgument(IllegalArgumentException exception)
    {
        return ResponseEntity.status(BAD_REQUEST).body(ValidationErrorResponse.invalidRequest(exception.getMessage()));
    }
}
