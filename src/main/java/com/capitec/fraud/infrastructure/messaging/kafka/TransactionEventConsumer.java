package com.capitec.fraud.infrastructure.messaging.kafka;

import com.capitec.fraud.application.TransactionEvaluationService;
import com.capitec.fraud.infrastructure.config.FraudKafkaProperties;
import com.capitec.fraud.infrastructure.config.FraudObservabilityProperties;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
public class TransactionEventConsumer
{

    private static final Logger LOG = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final TransactionEventMapper mapper;
    private final TransactionEvaluationService transactionEvaluationService;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTransactionEventMetrics metrics;
    private final FraudObservabilityProperties observabilityProperties;
    private final FraudKafkaProperties kafkaProperties;

    public TransactionEventConsumer(
            TransactionEventMapper mapper,
            TransactionEvaluationService transactionEvaluationService,
            ProcessedEventRepository processedEventRepository,
            KafkaTransactionEventMetrics metrics,
            FraudObservabilityProperties observabilityProperties,
            FraudKafkaProperties kafkaProperties)
    {
        this.mapper = mapper;
        this.transactionEvaluationService = transactionEvaluationService;
        this.processedEventRepository = processedEventRepository;
        this.metrics = metrics;
        this.observabilityProperties = observabilityProperties;
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(
            topics = "${fraud.kafka.transaction-events-topic}",
            groupId = "${fraud.kafka.consumer-group}",
            containerFactory = "transactionEventKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record)
    {
        metrics.recordConsumedEvent();

        try
        {
            var message = mapper.toMessage(record);
            var duplicateEvent = processedEventRepository.existsByEventId(message.eventId());

            withCorrelationId(message.correlationId(), () -> evaluate(record, message, duplicateEvent));
        }
        catch (RuntimeException ex)
        {
            metrics.recordFailedEvent();
            var headers = kafkaProperties.headers();
            withOptionalCorrelationId(optionalHeader(record, headers.correlationId()), () -> LOG.warn(
                    "event=kafka_transaction_event_failed topic={} partition={} offset={} eventId={} reason={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    optionalHeader(record, headers.eventId()).orElse("unknown"),
                    ex.getClass().getSimpleName()));
            throw ex;
        }
    }

    private void evaluate(
            ConsumerRecord<String, String> record,
            TransactionEventMessage message,
            boolean duplicateEvent)
    {
        LOG.info(
                "event=kafka_transaction_event_received topic={} partition={} offset={} eventId={} eventType={} schemaVersion={}",
                record.topic(),
                record.partition(),
                record.offset(),
                message.eventId(),
                message.eventType(),
                message.schemaVersion());

        var evaluation = transactionEvaluationService.evaluate(message.command());
        if (duplicateEvent)
        {
            metrics.recordDuplicateEvent();
            LOG.info(
                    "event=kafka_transaction_event_duplicate eventId={} transactionId={} decision={} riskScore={} riskLevel={}",
                    evaluation.transaction().eventId(),
                    evaluation.transaction().transactionId(),
                    evaluation.decision(),
                    evaluation.riskScore().value(),
                    evaluation.riskLevel());
            return;
        }

        metrics.recordEvaluatedEvent();
        LOG.info(
                "event=kafka_transaction_event_evaluated eventId={} transactionId={} decision={} riskScore={} riskLevel={}",
                evaluation.transaction().eventId(),
                evaluation.transaction().transactionId(),
                evaluation.decision(),
                evaluation.riskScore().value(),
                evaluation.riskLevel());
    }

    private void withCorrelationId(String correlationId, Runnable action)
    {
        MDC.put(observabilityProperties.correlationIdMdcKey(), correlationId);
        try
        {
            action.run();
        }
        finally
        {
            MDC.remove(observabilityProperties.correlationIdMdcKey());
        }
    }

    private void withOptionalCorrelationId(Optional<String> correlationId, Runnable action)
    {
        if (correlationId.isEmpty() || correlationId.get().isBlank())
        {
            action.run();
            return;
        }

        withCorrelationId(correlationId.get(), action);
    }

    private static Optional<String> optionalHeader(ConsumerRecord<String, String> record, String name)
    {
        var header = record.headers().lastHeader(name);
        if (header == null || header.value() == null || header.value().length == 0)
        {
            return Optional.empty();
        }

        return Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
