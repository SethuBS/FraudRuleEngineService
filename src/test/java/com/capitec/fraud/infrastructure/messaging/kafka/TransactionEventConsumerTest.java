package com.capitec.fraud.infrastructure.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.capitec.fraud.application.TransactionEvaluationCommand;
import com.capitec.fraud.application.TransactionEvaluationService;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.config.FraudKafkaProperties;
import com.capitec.fraud.infrastructure.config.FraudObservabilityProperties;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class TransactionEventConsumerTest
{

    private static final String TOPIC = "transaction-events";
    private static final String EVENT_ID = "event-kafka-1";
    private static final String CORRELATION_ID = "correlation-kafka-1";
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-10T08:30:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-10T09:00:00Z");
    private static final RiskPolicy RISK_POLICY = new RiskPolicy(
            RiskScore.of(25),
            RiskScore.of(50),
            RiskScore.of(75),
            RiskScore.of(100),
            RiskLevel.MEDIUM,
            RiskLevel.HIGH);
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

    private final ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPIC, 0, 0L, EVENT_ID, "{}");
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private TransactionEventMapper mapper;
    private TransactionEvaluationService transactionEvaluationService;
    private ProcessedEventRepository processedEventRepository;
    private TransactionEvaluationCommand command;
    private TransactionEvaluation evaluation;
    private TransactionEventConsumer consumer;

    @BeforeEach
    void setUp()
    {
        mapper = mock(TransactionEventMapper.class);
        transactionEvaluationService = mock(TransactionEvaluationService.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        command = TransactionEvaluationCommand.withoutRawPayload(sampleTransaction());
        evaluation = TransactionEvaluation.from(command.transaction(), List.of(), RISK_POLICY, EVALUATED_AT);
        consumer = new TransactionEventConsumer(
                mapper,
                transactionEvaluationService,
                processedEventRepository,
                new KafkaTransactionEventMetrics(meterRegistry),
                new FraudObservabilityProperties("correlationId", 128),
                KAFKA_PROPERTIES);
    }

    @Test
    void callsExistingEvaluationUseCaseForValidEvents()
    {
        when(mapper.toMessage(record)).thenReturn(message());
        when(processedEventRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(transactionEvaluationService.evaluate(command)).thenReturn(evaluation);

        consumer.consume(record);

        verify(transactionEvaluationService).evaluate(command);
        assertCounter("fraud.kafka.transaction.events.consumed", 1.0);
        assertCounter("fraud.kafka.transaction.events.evaluated", 1.0);
        assertCounter("fraud.kafka.transaction.events.duplicates", 0.0);
        assertCounter("fraud.kafka.transaction.events.failed", 0.0);
    }

    @Test
    void recordsDuplicateMetricWhenEventWasAlreadyProcessed()
    {
        when(mapper.toMessage(record)).thenReturn(message());
        when(processedEventRepository.existsByEventId(EVENT_ID)).thenReturn(true);
        when(transactionEvaluationService.evaluate(command)).thenReturn(evaluation);

        consumer.consume(record);

        verify(transactionEvaluationService).evaluate(command);
        assertCounter("fraud.kafka.transaction.events.consumed", 1.0);
        assertCounter("fraud.kafka.transaction.events.evaluated", 0.0);
        assertCounter("fraud.kafka.transaction.events.duplicates", 1.0);
        assertCounter("fraud.kafka.transaction.events.failed", 0.0);
    }

    @Test
    void invalidEventsFailBeforeRepositoryOrUseCaseInvocation()
    {
        when(mapper.toMessage(record)).thenThrow(new KafkaMessageValidationException("bad event"));

        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessage("bad event");

        verifyNoInteractions(processedEventRepository, transactionEvaluationService);
        assertCounter("fraud.kafka.transaction.events.consumed", 1.0);
        assertCounter("fraud.kafka.transaction.events.failed", 1.0);
    }

    private TransactionEventMessage message()
    {
        return new TransactionEventMessage(
                command,
                CORRELATION_ID,
                EVENT_ID,
                "TransactionEvaluationRequested",
                "1");
    }

    private void assertCounter(String name, double expectedCount)
    {
        assertThat(meterRegistry.counter(name).count()).isEqualTo(expectedCount);
    }

    private static Transaction sampleTransaction()
    {
        return new Transaction(
                EVENT_ID,
                "tx-kafka-1",
                "customer-kafka-1",
                "account-kafka-1",
                Money.of(new BigDecimal("15000.00"), "ZAR"),
                TransactionCategory.of("JEWELRY"),
                TRANSACTION_TIME,
                "merchant-kafka-1",
                "Kafka Jewellery",
                "MOBILE",
                "ZA",
                "device-kafka-1");
    }
}
