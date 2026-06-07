package com.capitec.fraud.api;

import com.capitec.fraud.infrastructure.config.FraudApiErrorProperties;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class ApiCorrelationIdProvider
{

    private final FraudApiErrorProperties properties;

    public ApiCorrelationIdProvider(FraudApiErrorProperties properties)
    {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request)
    {
        var correlationId = request.getHeader(properties.correlationIdHeader());
        if (correlationId == null || correlationId.isBlank())
        {
            return UUID.randomUUID().toString();
        }

        return correlationId.strip();
    }

    public void apply(HttpServletResponse response, String correlationId)
    {
        response.setHeader(properties.correlationIdHeader(), correlationId);
    }
}
