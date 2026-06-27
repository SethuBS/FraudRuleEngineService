package com.capitec.fraud.infrastructure.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.capitec.fraud.api.dto.TransactionEvaluationRequest;
import com.capitec.fraud.application.RawPayloadSanitizer;
import com.capitec.fraud.infrastructure.config.FraudKafkaProperties;
import com.capitec.fraud.infrastructure.config.FraudRawPayloadProperties;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

class TransactionEventMapperTest
{

    private static final String TOPIC = "transaction-events";
    private static final String EVENT_ID = "event-kafka-1";
    private static final String CORRELATION_ID = "correlation-kafka-1";
    private static final Instant NOW = Instant.parse("2026-06-10T09:00:00Z");
    private static final FraudKafkaProperties KAFKA_PROPERTIES = new FraudKafkaProperties(
            true,
            TOPIC,
            "transaction-events.dlq",
            "fraud-rule-engine-service",
            "TransactionEvaluationRequested",
            "1",
            new FraudKafkaProperties.Headers("correlationId", "eventId", "eventType", "schemaVersion"),
            new FraudKafkaProperties.Retry(3, Duration.ofSeconds(2)),
            new FraudKafkaProperties.Topics(1, 1));
    private static final FraudRawPayloadProperties RAW_PAYLOAD_PROPERTIES = new FraudRawPayloadProperties(
            Duration.ofDays(7),
            "MASKED",
            List.of("cardNumber", "authorization", "token"));

    private final JsonMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    private Validator validator;
    private TransactionEventMapper mapper;

    @BeforeEach
    void setUp()
    {
        validator = mock(Validator.class);
        when(validator.validate(any(TransactionEvaluationRequest.class))).thenReturn(Set.of());
        mapper = new TransactionEventMapper(
                objectMapper,
                validator,
                new RawPayloadSanitizer(objectMapper, RAW_PAYLOAD_PROPERTIES),
                RAW_PAYLOAD_PROPERTIES,
                KAFKA_PROPERTIES,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void mapsKafkaEventToEvaluationCommand()
    {
        var message = mapper.toMessage(record(validPayload(EVENT_ID), EVENT_ID));

        assertThat(message.correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(message.eventId()).isEqualTo(EVENT_ID);
        assertThat(message.eventType()).isEqualTo(KAFKA_PROPERTIES.expectedEventType());
        assertThat(message.schemaVersion()).isEqualTo(KAFKA_PROPERTIES.supportedSchemaVersion());
        assertThat(message.command().transaction().eventId()).isEqualTo(EVENT_ID);
        assertThat(message.command().transaction().transactionId()).isEqualTo("tx-kafka-1");
        assertThat(message.command().transaction().customerId()).isEqualTo("customer-kafka-1");
        assertThat(message.command().rawPayloadExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(message.command().sanitizedRawPayload())
                .contains("\"cardNumber\":\"MASKED\"")
                .doesNotContain("4111111111111111");
    }

    @Test
    void rejectsMissingRequiredHeaders()
    {
        var record = new ConsumerRecord<String, String>(TOPIC, 0, 0L, EVENT_ID, validPayload(EVENT_ID));

        assertThatThrownBy(() -> mapper.toMessage(record))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessage("Kafka header is required: correlationId");
    }

    @Test
    void rejectsHeaderEventIdThatDoesNotMatchPayload()
    {
        assertThatThrownBy(() -> mapper.toMessage(record(validPayload(EVENT_ID), "different-event")))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessage("Kafka eventId header must match payload eventId");
    }

    @Test
    void rejectsPayloadValidationErrors()
    {
        var violation = violation("eventId", "must not be blank");
        when(validator.validate(any(TransactionEvaluationRequest.class))).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> mapper.toMessage(record(validPayload(EVENT_ID), EVENT_ID)))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessage("Kafka transaction event failed validation: eventId=must not be blank");
    }

    @Test
    void rejectsInvalidJson()
    {
        assertThatThrownBy(() -> mapper.toMessage(record("not-json", EVENT_ID)))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessage("Kafka transaction event value must be valid JSON");
    }

    private static ConsumerRecord<String, String> record(String payload, String headerEventId)
    {
        var record = new ConsumerRecord<String, String>(TOPIC, 0, 0L, headerEventId, payload);
        addHeader(record, KAFKA_PROPERTIES.headers().correlationId(), CORRELATION_ID);
        addHeader(record, KAFKA_PROPERTIES.headers().eventId(), headerEventId);
        addHeader(record, KAFKA_PROPERTIES.headers().eventType(), KAFKA_PROPERTIES.expectedEventType());
        addHeader(record, KAFKA_PROPERTIES.headers().schemaVersion(), KAFKA_PROPERTIES.supportedSchemaVersion());

        return record;
    }

    private static void addHeader(ConsumerRecord<String, String> record, String name, String value)
    {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private static ConstraintViolation<TransactionEvaluationRequest> violation(String field, String message)
    {
        @SuppressWarnings("unchecked")
        var violation = (ConstraintViolation<TransactionEvaluationRequest>) mock(ConstraintViolation.class);
        var path = mock(Path.class);
        when(path.toString()).thenReturn(field);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);

        return violation;
    }

    private static String validPayload(String eventId)
    {
        return """
                {
                  "eventId": "%s",
                  "transactionId": "tx-kafka-1",
                  "customerId": "customer-kafka-1",
                  "accountId": "account-kafka-1",
                  "amount": 15000.00,
                  "currency": "ZAR",
                  "transactionTimestamp": "2026-06-10T08:30:00Z",
                  "merchantCategory": "JEWELRY",
                  "country": "ZA",
                  "channel": "MOBILE",
                  "merchantId": "merchant-kafka-1",
                  "merchantName": "Kafka Jewellery",
                  "deviceId": "device-kafka-1",
                  "cardNumber": "4111111111111111"
                }
                """.formatted(eventId);
    }
}
