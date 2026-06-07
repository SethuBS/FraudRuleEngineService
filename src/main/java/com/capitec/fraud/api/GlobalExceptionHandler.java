package com.capitec.fraud.api;

import static java.util.Comparator.comparing;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.capitec.fraud.api.ApiErrorResponse.FieldValidationError;
import com.capitec.fraud.application.TransactionEvaluationException;
import com.capitec.fraud.infrastructure.config.FraudApiErrorProperties;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler
{

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VALIDATION_FAILED_CODE = "VALIDATION_FAILED";
    private static final String RESOURCE_NOT_FOUND_CODE = "RESOURCE_NOT_FOUND";
    private static final String DUPLICATE_EVALUATION_CODE = "DUPLICATE_EVALUATION";
    private static final String ACCESS_DENIED_CODE = "ACCESS_DENIED";
    private static final String INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR";

    private final FraudApiErrorProperties properties;
    private final ApiCorrelationIdProvider correlationIdProvider;

    public GlobalExceptionHandler(
            FraudApiErrorProperties properties,
            ApiCorrelationIdProvider correlationIdProvider)
    {
        this.properties = properties;
        this.correlationIdProvider = correlationIdProvider;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request)
    {
        var fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(comparing(FieldError::getField))
                .map(error -> new FieldValidationError(error.getField(), safeMessage(error.getDefaultMessage())))
                .toList();

        return errorResponse(BAD_REQUEST, VALIDATION_FAILED_CODE, properties.validationFailedMessage(), fieldErrors, request);
    }

    @ExceptionHandler(RequestValidationException.class)
    ResponseEntity<ApiErrorResponse> handleRequestValidation(
            RequestValidationException exception,
            HttpServletRequest request)
    {
        return errorResponse(
                BAD_REQUEST,
                VALIDATION_FAILED_CODE,
                properties.validationFailedMessage(),
                exception.getFieldErrors(),
                request);
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request)
    {
        return errorResponse(BAD_REQUEST, VALIDATION_FAILED_CODE, properties.malformedRequestMessage(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request)
    {
        var fieldError = new FieldValidationError(exception.getName(), properties.invalidParameterMessage());

        return errorResponse(BAD_REQUEST, VALIDATION_FAILED_CODE, properties.validationFailedMessage(), List.of(fieldError), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request)
    {
        return errorResponse(NOT_FOUND, RESOURCE_NOT_FOUND_CODE, exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler({
        DataIntegrityViolationException.class,
        TransactionEvaluationException.class
    })
    ResponseEntity<ApiErrorResponse> handleDuplicateEvaluationRace(
            Exception exception,
            HttpServletRequest request)
    {
        logHandledException(CONFLICT.value(), DUPLICATE_EVALUATION_CODE, exception, request);

        return errorResponse(CONFLICT, DUPLICATE_EVALUATION_CODE, properties.duplicateEvaluationMessage(), List.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request)
    {
        return errorResponse(FORBIDDEN, ACCESS_DENIED_CODE, properties.forbiddenMessage(), List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request)
    {
        logHandledException(INTERNAL_SERVER_ERROR.value(), INTERNAL_SERVER_ERROR_CODE, exception, request);

        return errorResponse(
                INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_CODE,
                properties.internalServerErrorMessage(),
                List.of(),
                request);
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(
            org.springframework.http.HttpStatus status,
            String code,
            String message,
            List<FieldValidationError> fieldErrors,
            HttpServletRequest request)
    {
        var correlationId = correlationIdProvider.resolve(request);
        var response = ApiErrorResponse.of(code, message, correlationId, fieldErrors);

        return ResponseEntity.status(status)
                .header(properties.correlationIdHeader(), correlationId)
                .body(response);
    }

    private static String safeMessage(String message)
    {
        return message == null ? "" : message;
    }

    private static void logHandledException(
            int statusCode,
            String code,
            Exception exception,
            HttpServletRequest request)
    {
        LOG.error(
                "Handled API exception status={} code={} exceptionType={} method={} path={}",
                statusCode,
                code,
                exception.getClass().getName(),
                request.getMethod(),
                request.getRequestURI());
    }
}
