package com.capitec.fraud.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.infrastructure.config.JpaAuditingConfiguration;
import com.capitec.fraud.infrastructure.persistence.entity.ProcessedEventEntity;
import com.capitec.fraud.infrastructure.persistence.entity.TransactionEntity;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.support.PostgresIntegrationTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    JpaAuditingConfiguration.class,
    RawPayloadCleanupService.class,
    RawPayloadCleanupServiceIntegrationTest.FixedClockConfiguration.class
})
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class RawPayloadCleanupServiceIntegrationTest extends PostgresIntegrationTest
{

    private static final Instant CLEANUP_TIME = Instant.parse("2026-06-14T09:00:00Z");
    private static final Instant EXPIRED_AT = Instant.parse("2026-06-14T08:59:59Z");
    private static final Instant FUTURE_EXPIRES_AT = Instant.parse("2026-06-14T09:00:01Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final String EXPIRED_TRANSACTION_ID = "tx-expired";
    private static final String FUTURE_TRANSACTION_ID = "tx-future";
    private static final String EXPIRED_EVENT_ID = "event-expired";
    private static final String FUTURE_EVENT_ID = "event-future";
    private static final String SANITIZED_EXPIRED_PAYLOAD = "{\"eventId\":\"event-expired\"}";
    private static final String SANITIZED_FUTURE_PAYLOAD = "{\"eventId\":\"event-future\"}";

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = createPostgresContainer();

    @jakarta.annotation.Resource
    private RawPayloadCleanupService cleanupService;

    @jakarta.annotation.Resource
    private TransactionRepository transactionRepository;

    @jakarta.annotation.Resource
    private ProcessedEventRepository processedEventRepository;

    @jakarta.annotation.Resource
    private EntityManager entityManager;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry)
    {
        registerDatasourceProperties(registry, POSTGRESQL);
    }

    @Test
    void clearsExpiredPayloadsWithoutDeletingEvaluationRecords()
    {
        transactionRepository.save(transaction(EXPIRED_EVENT_ID, EXPIRED_TRANSACTION_ID, SANITIZED_EXPIRED_PAYLOAD, EXPIRED_AT));
        transactionRepository.save(transaction(FUTURE_EVENT_ID, FUTURE_TRANSACTION_ID, SANITIZED_FUTURE_PAYLOAD, FUTURE_EXPIRES_AT));
        processedEventRepository.save(processedEvent(EXPIRED_EVENT_ID, EXPIRED_TRANSACTION_ID, SANITIZED_EXPIRED_PAYLOAD, EXPIRED_AT));
        processedEventRepository.save(processedEvent(FUTURE_EVENT_ID, FUTURE_TRANSACTION_ID, SANITIZED_FUTURE_PAYLOAD, FUTURE_EXPIRES_AT));
        entityManager.flush();
        entityManager.clear();

        var result = cleanupService.clearExpiredPayloads(CLEANUP_TIME);

        entityManager.flush();
        entityManager.clear();

        assertThat(result.transactionPayloadsCleared()).isEqualTo(1);
        assertThat(result.processedEventPayloadsCleared()).isEqualTo(1);
        assertThat(result.totalPayloadsCleared()).isEqualTo(2);

        assertThat(transactionRepository.findByTransactionId(EXPIRED_TRANSACTION_ID))
                .hasValueSatisfying(transaction ->
                {
                    assertThat(transaction.getSanitizedRawPayload()).isNull();
                    assertThat(transaction.getRawPayloadExpiresAt()).isNull();
                    assertThat(transaction.getTransactionId()).isEqualTo(EXPIRED_TRANSACTION_ID);
                    assertThat(transaction.getCustomerId()).isEqualTo("customer-1");
                    assertThat(transaction.getAmount()).isEqualByComparingTo("100.50");
                });
        assertThat(processedEventRepository.findByEventId(EXPIRED_EVENT_ID))
                .hasValueSatisfying(processedEvent ->
                {
                    assertThat(processedEvent.getSanitizedRawPayload()).isNull();
                    assertThat(processedEvent.getRawPayloadExpiresAt()).isNull();
                    assertThat(processedEvent.getTransactionId()).isEqualTo(EXPIRED_TRANSACTION_ID);
                    assertThat(processedEvent.getProcessingStatus()).isEqualTo("COMPLETED");
                    assertThat(processedEvent.getEvaluatedAt()).isEqualTo(EVALUATED_AT);
                });
        assertThat(transactionRepository.findByTransactionId(FUTURE_TRANSACTION_ID))
                .hasValueSatisfying(transaction ->
                {
                    assertThat(compactJson(transaction.getSanitizedRawPayload())).isEqualTo(SANITIZED_FUTURE_PAYLOAD);
                    assertThat(transaction.getRawPayloadExpiresAt()).isEqualTo(FUTURE_EXPIRES_AT);
                });
        assertThat(processedEventRepository.findByEventId(FUTURE_EVENT_ID))
                .hasValueSatisfying(processedEvent ->
                {
                    assertThat(compactJson(processedEvent.getSanitizedRawPayload())).isEqualTo(SANITIZED_FUTURE_PAYLOAD);
                    assertThat(processedEvent.getRawPayloadExpiresAt()).isEqualTo(FUTURE_EXPIRES_AT);
                });
    }

    private static String compactJson(String payload)
    {
        return payload.replaceAll("\\s+", "");
    }

    private static TransactionEntity transaction(
            String eventId,
            String transactionId,
            String sanitizedRawPayload,
            Instant rawPayloadExpiresAt)
    {
        return new TransactionEntity(
                eventId,
                transactionId,
                "customer-1",
                "account-1",
                new BigDecimal("100.50"),
                "ZAR",
                Instant.parse("2026-06-07T08:00:00Z"),
                "GROCERY",
                "ZA",
                "MOBILE",
                "merchant-1",
                "Corner Shop",
                "device-1",
                sanitizedRawPayload,
                rawPayloadExpiresAt);
    }

    private static ProcessedEventEntity processedEvent(
            String eventId,
            String transactionId,
            String sanitizedRawPayload,
            Instant rawPayloadExpiresAt)
    {
        return new ProcessedEventEntity(
                eventId,
                transactionId,
                "COMPLETED",
                EVALUATED_AT,
                sanitizedRawPayload,
                rawPayloadExpiresAt);
    }

    @TestConfiguration
    static class FixedClockConfiguration
    {

        @Bean
        Clock fixedClock()
        {
            return Clock.fixed(CLEANUP_TIME, ZoneOffset.UTC);
        }
    }
}
