package com.capitec.fraud.infrastructure.messaging.kafka;

import static java.util.Comparator.comparing;

import com.capitec.fraud.api.ApiErrorResponse.FieldValidationError;
import com.capitec.fraud.api.dto.TransactionEvaluationRequest;
import com.capitec.fraud.application.RawPayloadSanitizer;
import com.capitec.fraud.application.TransactionEvaluationCommand;
import com.capitec.fraud.infrastructure.config.FraudKafkaProperties;
import com.capitec.fraud.infrastructure.config.FraudRawPayloadProperties;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
public class TransactionEventMapper
{

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final RawPayloadSanitizer rawPayloadSanitizer;
    private final FraudRawPayloadProperties rawPayloadProperties;
    private final FraudKafkaProperties kafkaProperties;
    private final Clock clock;

    public TransactionEventMapper(
            ObjectMapper objectMapper,
            Validator validator,
            RawPayloadSanitizer rawPayloadSanitizer,
            FraudRawPayloadProperties rawPayloadProperties,
            FraudKafkaProperties kafkaProperties,
            Clock clock)
    {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.rawPayloadSanitizer = rawPayloadSanitizer;
        this.rawPayloadProperties = rawPayloadProperties;
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
    }

    public TransactionEventMessage toMessage(ConsumerRecord<String, String> record)
    {
        var headers = kafkaProperties.headers();
        var correlationId = requiredHeader(record.headers(), headers.correlationId());
        var headerEventId = requiredHeader(record.headers(), headers.eventId());
        var eventType = requiredHeader(record.headers(), headers.eventType());
        var schemaVersion = requiredHeader(record.headers(), headers.schemaVersion());
        validateHeaderValue(headers.eventType(), eventType, kafkaProperties.expectedEventType());
        validateHeaderValue(headers.schemaVersion(), schemaVersion, kafkaProperties.supportedSchemaVersion());

        var request = toRequest(record.value());
        if (!headerEventId.equals(request.eventId()))
        {
            throw new KafkaMessageValidationException("Kafka eventId header must match payload eventId");
        }

        return new TransactionEventMessage(
                new TransactionEvaluationCommand(
                        request.toDomain(),
                        rawPayloadSanitizer.sanitize(record.value()),
                        Instant.now(clock).plus(rawPayloadProperties.retentionDuration())),
                correlationId,
                headerEventId,
                eventType,
                schemaVersion);
    }

    private TransactionEvaluationRequest toRequest(String rawPayload)
    {
        if (rawPayload == null || rawPayload.isBlank())
        {
            throw new KafkaMessageValidationException("Kafka transaction event value is required");
        }

        try
        {
            var request = objectMapper.readValue(rawPayload, TransactionEvaluationRequest.class);
            validate(request);

            return request;
        }
        catch (JsonProcessingException ex)
        {
            throw new KafkaMessageValidationException("Kafka transaction event value must be valid JSON", ex);
        }
    }

    private void validate(TransactionEvaluationRequest request)
    {
        var fieldErrors = validator.validate(request)
                .stream()
                .sorted(comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> new FieldValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();

        if (!fieldErrors.isEmpty())
        {
            var details = fieldErrors.stream()
                    .map(error -> error.field() + "=" + error.message())
                    .collect(Collectors.joining(", "));
            throw new KafkaMessageValidationException("Kafka transaction event failed validation: " + details);
        }
    }

    private static String requiredHeader(Headers headers, String name)
    {
        var header = headers.lastHeader(name);
        if (header == null || header.value() == null || header.value().length == 0)
        {
            throw new KafkaMessageValidationException("Kafka header is required: " + name);
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static void validateHeaderValue(String name, String actualValue, String expectedValue)
    {
        if (!expectedValue.equals(actualValue))
        {
            throw new KafkaMessageValidationException("Kafka header " + name + " must be " + expectedValue);
        }
    }
}
