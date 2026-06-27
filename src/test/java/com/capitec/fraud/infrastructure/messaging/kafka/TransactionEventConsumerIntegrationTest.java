package com.capitec.fraud.infrastructure.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.RuleEvaluationRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.support.PostgresIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "fraud.kafka.enabled=true",
    "fraud.kafka.consumer-group=fraud-rule-engine-service-it",
    "spring.kafka.consumer.group-id=fraud-rule-engine-service-it",
    "fraud.kafka.transaction-events-topic=transaction-events-it",
    "fraud.kafka.dead-letter-topic=transaction-events-it.dlq",
    "fraud.evaluation.maximum-score=100",
    "fraud.evaluation.risk-thresholds.medium=25",
    "fraud.evaluation.risk-thresholds.high=50",
    "fraud.evaluation.risk-thresholds.critical=75",
    "fraud.evaluation.decision-thresholds.review=MEDIUM",
    "fraud.evaluation.decision-thresholds.flagged=HIGH",
    "fraud.rule-catalog.enabled-by-default=true",
    "fraud.rule-catalog.seed-on-startup=true",
    "fraud.raw-payload.cleanup.enabled=false",
    "fraud.rules.high-value-transaction.threshold-amount=100.00",
    "fraud.rules.high-value-transaction.default-score=55",
    "fraud.rules.high-value-transaction.severity=HIGH",
    "fraud.rules.risky-merchant-category.risk-categories=GAMBLING,CRYPTO,JEWELRY",
    "fraud.rules.risky-merchant-category.default-score=30",
    "fraud.rules.risky-merchant-category.severity=MEDIUM",
    "fraud.rules.suspicious-merchant.merchant-ids=MERCHANT-WATCHLIST",
    "fraud.rules.suspicious-merchant.merchant-name-fragments=WATCHLISTED,HIGH RISK TRADERS",
    "fraud.rules.suspicious-merchant.default-score=50",
    "fraud.rules.suspicious-merchant.severity=HIGH"
})
class TransactionEventConsumerIntegrationTest extends PostgresIntegrationTest
{

    private static final DockerImageName REDPANDA_IMAGE = DockerImageName.parse("redpandadata/redpanda:v26.1.11")
            .asCompatibleSubstituteFor("docker.redpanda.com/redpandadata/redpanda");
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(250);
    private static final int EXPECTED_EVALUATED_RULE_COUNT = 6;
    private static final int EXPECTED_MATCHED_RULE_COUNT = 3;
    private static final String VALID_EVENT_ID = "event-kafka-it-valid-1";
    private static final String VALID_TRANSACTION_ID = "tx-kafka-it-valid-1";
    private static final String DUPLICATE_EVENT_ID = "event-kafka-it-duplicate-1";
    private static final String DUPLICATE_TRANSACTION_ID = "tx-kafka-it-duplicate-1";
    private static final String INVALID_EVENT_ID = "event-kafka-it-invalid-1";
    private static final String INVALID_TRANSACTION_ID = "tx-kafka-it-invalid-1";
    private static final String EVENT_TYPE = "TransactionEvaluationRequested";
    private static final String SCHEMA_VERSION = "1";
    private static final String KAFKA_DUPLICATE_COUNTER = "fraud.kafka.transaction.events.duplicates";
    private static final String CURRENCY = "ZAR";

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = createPostgresContainer();

    @Container
    private static final RedpandaContainer REDPANDA = new RedpandaContainer(REDPANDA_IMAGE);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private RuleEvaluationRepository ruleEvaluationRepository;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${fraud.kafka.transaction-events-topic}")
    private String transactionEventsTopic;

    @Value("${fraud.kafka.dead-letter-topic}")
    private String deadLetterTopic;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry)
    {
        registerDatasourceProperties(registry, POSTGRESQL);
        registry.add("spring.kafka.bootstrap-servers", REDPANDA::getBootstrapServers);
    }

    @BeforeEach
    void resetDatabase()
    {
        fraudAlertRepository.deleteAll();
        ruleEvaluationRepository.deleteAll();
        transactionRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void validEventIsConsumedAndEvaluated()
            throws Exception
    {
        publishValidEvent(VALID_EVENT_ID, VALID_TRANSACTION_ID);

        await("transaction persisted", () -> transactionRepository.findByTransactionId(VALID_TRANSACTION_ID).isPresent());

        var transaction = transactionRepository.findByTransactionId(VALID_TRANSACTION_ID).orElseThrow();
        assertThat(transaction.getEventId()).isEqualTo(VALID_EVENT_ID);
        assertThat(transaction.getSanitizedRawPayload()).contains(VALID_TRANSACTION_ID);
        assertThat(processedEventRepository.findByEventId(VALID_EVENT_ID)).isPresent();
        assertThat(ruleEvaluationRepository.findByTransactionTransactionIdOrderByCreatedAtAsc(VALID_TRANSACTION_ID))
                .hasSize(EXPECTED_EVALUATED_RULE_COUNT);
        assertThat(ruleEvaluationRepository.findByTransactionTransactionIdAndMatchedOrderByCreatedAtAsc(
                VALID_TRANSACTION_ID,
                true))
                .hasSize(EXPECTED_MATCHED_RULE_COUNT);
        assertThat(fraudAlertRepository.findByTransactionTransactionIdOrderByCreatedAtDesc(VALID_TRANSACTION_ID))
                .hasSize(1);
    }

    @Test
    void duplicateEventReturnsPreviousResultWithoutDuplicateRecords()
            throws Exception
    {
        publishValidEvent(DUPLICATE_EVENT_ID, DUPLICATE_TRANSACTION_ID);
        await("first duplicate scenario transaction persisted",
                () -> transactionRepository.findByTransactionId(DUPLICATE_TRANSACTION_ID).isPresent());

        publishValidEvent(DUPLICATE_EVENT_ID, DUPLICATE_TRANSACTION_ID);
        await("duplicate event metric recorded", () -> counterValue(KAFKA_DUPLICATE_COUNTER) >= 1.0D);

        assertThat(transactionRepository.findAll())
                .extracting(transaction -> transaction.getTransactionId())
                .containsExactly(DUPLICATE_TRANSACTION_ID);
        assertThat(processedEventRepository.findAll())
                .extracting(processedEvent -> processedEvent.getEventId())
                .containsExactly(DUPLICATE_EVENT_ID);
        assertThat(ruleEvaluationRepository.findByTransactionTransactionIdOrderByCreatedAtAsc(DUPLICATE_TRANSACTION_ID))
                .hasSize(EXPECTED_EVALUATED_RULE_COUNT);
        assertThat(fraudAlertRepository.findByTransactionTransactionIdOrderByCreatedAtDesc(DUPLICATE_TRANSACTION_ID))
                .hasSize(1);
    }

    @Test
    void invalidEventIsDeadLetteredAndDoesNotCreateTransaction()
            throws Exception
    {
        try (var consumer = deadLetterConsumer())
        {
            consumer.subscribe(List.of(deadLetterTopic));
            publishInvalidEventMissingSchemaVersion();

            var deadLetterRecord = pollForDeadLetterRecord(consumer, INVALID_EVENT_ID);

            assertThat(deadLetterRecord.key()).isEqualTo(INVALID_EVENT_ID);
            assertThat(deadLetterRecord.value()).contains(INVALID_TRANSACTION_ID);
            assertThat(transactionRepository.findByTransactionId(INVALID_TRANSACTION_ID)).isEmpty();
            assertThat(processedEventRepository.findByEventId(INVALID_EVENT_ID)).isEmpty();
        }
    }

    private void publishValidEvent(String eventId, String transactionId)
            throws Exception
    {
        var record = transactionRecord(eventId, transactionId, highRiskPayload(eventId, transactionId));
        addRequiredHeaders(record, eventId);

        kafkaTemplate.send(record).get();
    }

    private void publishInvalidEventMissingSchemaVersion()
            throws Exception
    {
        var record = transactionRecord(
                INVALID_EVENT_ID,
                INVALID_TRANSACTION_ID,
                lowRiskPayload(INVALID_EVENT_ID, INVALID_TRANSACTION_ID));
        addHeader(record, "correlationId", "kafka-it-invalid");
        addHeader(record, "eventId", INVALID_EVENT_ID);
        addHeader(record, "eventType", EVENT_TYPE);

        kafkaTemplate.send(record).get();
    }

    private ProducerRecord<String, String> transactionRecord(String eventId, String transactionId, String payload)
    {
        return new ProducerRecord<>(transactionEventsTopic, null, Instant.now().toEpochMilli(), eventId, payload);
    }

    private static void addRequiredHeaders(ProducerRecord<String, String> record, String eventId)
    {
        addHeader(record, "correlationId", "kafka-it-" + eventId);
        addHeader(record, "eventId", eventId);
        addHeader(record, "eventType", EVENT_TYPE);
        addHeader(record, "schemaVersion", SCHEMA_VERSION);
    }

    private static void addHeader(ProducerRecord<String, String> record, String name, String value)
    {
        record.headers().add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)));
    }

    private KafkaConsumer<String, String> deadLetterConsumer()
    {
        var properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, REDPANDA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "fraud-rule-engine-dlq-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        return new KafkaConsumer<>(properties);
    }

    private static ConsumerRecord<String, String> pollForDeadLetterRecord(
            KafkaConsumer<String, String> consumer,
            String expectedEventId)
    {
        var deadline = Instant.now().plus(AWAIT_TIMEOUT);
        while (Instant.now().isBefore(deadline))
        {
            var records = consumer.poll(AWAIT_POLL_INTERVAL);
            for (var record : records)
            {
                if (expectedEventId.equals(record.key()))
                {
                    return record;
                }
            }
        }

        return fail("Timed out waiting for dead-letter record with key " + expectedEventId);
    }

    private void await(String description, BooleanSupplier condition)
            throws InterruptedException
    {
        var deadline = Instant.now().plus(AWAIT_TIMEOUT);
        while (Instant.now().isBefore(deadline))
        {
            if (condition.getAsBoolean())
            {
                return;
            }
            Thread.sleep(AWAIT_POLL_INTERVAL.toMillis());
        }

        fail("Timed out waiting for " + description);
    }

    private double counterValue(String name)
    {
        var counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0D : counter.count();
    }

    private static String highRiskPayload(String eventId, String transactionId)
    {
        return """
                {
                  "eventId": "%s",
                  "transactionId": "%s",
                  "customerId": "customer-kafka-it",
                  "accountId": "account-kafka-it",
                  "amount": 250.00,
                  "currency": "%s",
                  "transactionTimestamp": "2026-06-10T08:30:00Z",
                  "merchantCategory": "JEWELRY",
                  "country": "ZA",
                  "channel": "MOBILE",
                  "merchantId": "MERCHANT-WATCHLIST",
                  "merchantName": "High Risk Traders",
                  "deviceId": "device-kafka-it"
                }
                """.formatted(eventId, transactionId, CURRENCY);
    }

    private static String lowRiskPayload(String eventId, String transactionId)
    {
        return """
                {
                  "eventId": "%s",
                  "transactionId": "%s",
                  "customerId": "customer-kafka-it-invalid",
                  "accountId": "account-kafka-it-invalid",
                  "amount": 10.00,
                  "currency": "%s",
                  "transactionTimestamp": "2026-06-10T08:30:00Z",
                  "merchantCategory": "GROCERY",
                  "country": "ZA"
                }
                """.formatted(eventId, transactionId, CURRENCY);
    }
}
