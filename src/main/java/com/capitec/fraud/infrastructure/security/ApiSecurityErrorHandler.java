package com.capitec.fraud.infrastructure.security;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.capitec.fraud.api.ApiCorrelationIdProvider;
import com.capitec.fraud.api.ApiErrorResponse;
import com.capitec.fraud.infrastructure.config.FraudApiErrorProperties;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler
{

    private static final String AUTHENTICATION_REQUIRED_CODE = "AUTHENTICATION_REQUIRED";
    private static final String ACCESS_DENIED_CODE = "ACCESS_DENIED";

    private final FraudApiErrorProperties properties;
    private final ApiCorrelationIdProvider correlationIdProvider;
    private final ObjectMapper objectMapper;

    public ApiSecurityErrorHandler(
            FraudApiErrorProperties properties,
            ApiCorrelationIdProvider correlationIdProvider,
            ObjectMapper objectMapper)
    {
        this.properties = properties;
        this.correlationIdProvider = correlationIdProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException
    {
        writeError(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                AUTHENTICATION_REQUIRED_CODE,
                properties.authenticationRequiredMessage());
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException
    {
        writeError(
                request,
                response,
                HttpStatus.FORBIDDEN,
                ACCESS_DENIED_CODE,
                properties.forbiddenMessage());
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message)
            throws IOException
    {
        var correlationId = correlationIdProvider.resolve(request);
        var errorResponse = ApiErrorResponse.of(code, message, correlationId, List.of());

        response.setStatus(status.value());
        response.setContentType(APPLICATION_JSON_VALUE);
        correlationIdProvider.apply(response, correlationId);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
