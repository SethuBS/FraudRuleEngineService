package com.capitec.fraud.api;

import com.capitec.fraud.infrastructure.config.FraudApiErrorProperties;
import com.capitec.fraud.infrastructure.config.FraudObservabilityProperties;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class ApiCorrelationIdProvider
{

    public static final String CORRELATION_ID_ATTRIBUTE = ApiCorrelationIdProvider.class.getName() + ".correlationId";

    private final FraudApiErrorProperties properties;
    private final FraudObservabilityProperties observabilityProperties;

    public ApiCorrelationIdProvider(
            FraudApiErrorProperties properties,
            FraudObservabilityProperties observabilityProperties)
    {
        this.properties = properties;
        this.observabilityProperties = observabilityProperties;
    }

    public String resolve(HttpServletRequest request)
    {
        var existingCorrelationId = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        if (existingCorrelationId instanceof String correlationId && !correlationId.isBlank())
        {
            return correlationId;
        }

        var correlationId = resolveIncomingCorrelationId(request);
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);

        return correlationId;
    }

    public void apply(HttpServletResponse response, String correlationId)
    {
        response.setHeader(properties.correlationIdHeader(), correlationId);
    }

    private String resolveIncomingCorrelationId(HttpServletRequest request)
    {
        var correlationId = request.getHeader(properties.correlationIdHeader());
        if (correlationId == null || correlationId.isBlank())
        {
            return UUID.randomUUID().toString();
        }

        var trimmedCorrelationId = correlationId.strip();
        if (trimmedCorrelationId.length() > observabilityProperties.correlationIdMaxLength()
                || containsControlCharacter(trimmedCorrelationId))
        {
            return UUID.randomUUID().toString();
        }

        return trimmedCorrelationId;
    }

    private static boolean containsControlCharacter(String value)
    {
        return value.chars().anyMatch(Character::isISOControl);
    }
}
