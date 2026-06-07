package com.capitec.fraud.infrastructure.observability;

import com.capitec.fraud.api.ApiCorrelationIdProvider;
import com.capitec.fraud.infrastructure.config.FraudObservabilityProperties;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter
{

    private final ApiCorrelationIdProvider correlationIdProvider;
    private final FraudObservabilityProperties properties;

    public CorrelationIdFilter(
            ApiCorrelationIdProvider correlationIdProvider,
            FraudObservabilityProperties properties)
    {
        this.correlationIdProvider = correlationIdProvider;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException
    {
        var correlationId = correlationIdProvider.resolve(request);
        correlationIdProvider.apply(response, correlationId);
        MDC.put(properties.correlationIdMdcKey(), correlationId);

        try
        {
            filterChain.doFilter(request, response);
        }
        finally
        {
            MDC.remove(properties.correlationIdMdcKey());
        }
    }
}
