package com.capitec.fraud.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.api.ApiCorrelationIdProvider;
import com.capitec.fraud.infrastructure.config.FraudApiErrorProperties;
import com.capitec.fraud.infrastructure.config.FraudObservabilityProperties;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest
{

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID = "support-correlation-id";
    private static final String CORRELATION_MDC_KEY = "correlationId";
    private static final int CORRELATION_ID_MAX_LENGTH = 128;

    @AfterEach
    void clearMdc()
    {
        MDC.clear();
    }

    @Test
    void usesProvidedCorrelationIdInMdcAndResponseHeader()
            throws Exception
    {
        var request = new MockHttpServletRequest("GET", "/api/test");
        var response = new MockHttpServletResponse();
        var mdcValueDuringRequest = new AtomicReference<String>();
        request.addHeader(CORRELATION_HEADER, " " + CORRELATION_ID + " ");

        filter().doFilter(request, response, capturingChain(mdcValueDuringRequest));

        assertThat(mdcValueDuringRequest).hasValue(CORRELATION_ID);
        assertThat(response.getHeader(CORRELATION_HEADER)).isEqualTo(CORRELATION_ID);
        assertThat(request.getAttribute(ApiCorrelationIdProvider.CORRELATION_ID_ATTRIBUTE)).isEqualTo(CORRELATION_ID);
        assertThat(MDC.get(CORRELATION_MDC_KEY)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing()
            throws Exception
    {
        var request = new MockHttpServletRequest("GET", "/api/test");
        var response = new MockHttpServletResponse();
        var mdcValueDuringRequest = new AtomicReference<String>();

        filter().doFilter(request, response, capturingChain(mdcValueDuringRequest));

        assertThat(mdcValueDuringRequest.get()).isNotBlank();
        assertThat(response.getHeader(CORRELATION_HEADER)).isEqualTo(mdcValueDuringRequest.get());
        assertThat(request.getAttribute(ApiCorrelationIdProvider.CORRELATION_ID_ATTRIBUTE))
                .isEqualTo(mdcValueDuringRequest.get());
        assertThat(MDC.get(CORRELATION_MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeCorrelationId()
            throws Exception
    {
        var request = new MockHttpServletRequest("GET", "/api/test");
        var response = new MockHttpServletResponse();
        var mdcValueDuringRequest = new AtomicReference<String>();
        request.addHeader(CORRELATION_HEADER, "line-one\nline-two");

        filter().doFilter(request, response, capturingChain(mdcValueDuringRequest));

        assertThat(mdcValueDuringRequest.get()).isNotBlank();
        assertThat(mdcValueDuringRequest.get()).isNotEqualTo("line-one\nline-two");
        assertThat(response.getHeader(CORRELATION_HEADER)).isEqualTo(mdcValueDuringRequest.get());
    }

    private static CorrelationIdFilter filter()
    {
        var observabilityProperties = new FraudObservabilityProperties(
                CORRELATION_MDC_KEY,
                CORRELATION_ID_MAX_LENGTH);
        var correlationIdProvider = new ApiCorrelationIdProvider(
                errorProperties(),
                observabilityProperties);

        return new CorrelationIdFilter(correlationIdProvider, observabilityProperties);
    }

    private static FraudApiErrorProperties errorProperties()
    {
        return new FraudApiErrorProperties(
                CORRELATION_HEADER,
                "Request validation failed",
                "Request body or parameters are invalid",
                "Request parameter has an invalid value",
                "Duplicate event or transaction could not be resolved safely",
                "Authentication is required",
                "Required scope is missing",
                "An unexpected error occurred");
    }

    private static FilterChain capturingChain(AtomicReference<String> mdcValueDuringRequest)
    {
        return new FilterChain()
        {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException
            {
                mdcValueDuringRequest.set(MDC.get(CORRELATION_MDC_KEY));
            }
        };
    }
}
