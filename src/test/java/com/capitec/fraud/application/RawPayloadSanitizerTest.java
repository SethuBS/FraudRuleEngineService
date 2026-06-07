package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capitec.fraud.infrastructure.config.FraudRawPayloadProperties;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class RawPayloadSanitizerTest
{

    private static final String REDACTED_VALUE = "MASKED";

    private final RawPayloadSanitizer sanitizer = new RawPayloadSanitizer(
            new ObjectMapper(),
            new FraudRawPayloadProperties(
                    Duration.ofDays(7),
                    REDACTED_VALUE,
                    List.of(
                            "accountNumber",
                            "authorization",
                            "cardNumber",
                            "email",
                            "token")));

    @Test
    void redactsConfiguredSensitiveFieldsRecursively()
    {
        var sanitizedPayload = sanitizer.sanitize("""
                {
                  "eventId": "event-1",
                  "customerId": "customer-1",
                  "accountId": "account-1",
                  "accountNumber": "1234567890",
                  "payment": {
                    "cardNumber": "4111111111111111",
                    "authorization": "Bearer secret-token"
                  },
                  "contacts": [
                    {
                      "email": "customer@example.com",
                      "token": "contact-token"
                    }
                  ]
                }
                """);

        assertThat(sanitizedPayload)
                .contains("\"eventId\":\"event-1\"")
                .contains("\"customerId\":\"customer-1\"")
                .contains("\"accountId\":\"account-1\"")
                .contains("\"accountNumber\":\"MASKED\"")
                .contains("\"cardNumber\":\"MASKED\"")
                .contains("\"authorization\":\"MASKED\"")
                .contains("\"email\":\"MASKED\"")
                .contains("\"token\":\"MASKED\"")
                .doesNotContain("1234567890")
                .doesNotContain("4111111111111111")
                .doesNotContain("secret-token")
                .doesNotContain("customer@example.com")
                .doesNotContain("contact-token");
    }

    @Test
    void rejectsInvalidJson()
    {
        assertThatThrownBy(() -> sanitizer.sanitize("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request body must be valid JSON");
    }
}
