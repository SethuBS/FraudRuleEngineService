package com.capitec.fraud.api;

import static java.util.Comparator.comparing;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.capitec.fraud.api.ValidationErrorResponse.FieldValidationError;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(RequestValidationException.class)
    ResponseEntity<ValidationErrorResponse> handleRequestValidation(RequestValidationException exception)
    {
        return ResponseEntity.status(BAD_REQUEST).body(ValidationErrorResponse.invalidRequest(exception.getFieldErrors()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ValidationErrorResponse> handleIllegalArgument(IllegalArgumentException exception)
    {
        return ResponseEntity.status(BAD_REQUEST).body(ValidationErrorResponse.invalidRequest(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ValidationErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception)
    {
        var fieldError = new FieldValidationError(exception.getName(), "Request parameter has an invalid value");

        return ResponseEntity.status(BAD_REQUEST).body(ValidationErrorResponse.invalidRequest(List.of(fieldError)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ValidationErrorResponse> handleResourceNotFound(ResourceNotFoundException exception)
    {
        return ResponseEntity.status(NOT_FOUND).body(ValidationErrorResponse.resourceNotFound(exception.getMessage()));
    }
}
