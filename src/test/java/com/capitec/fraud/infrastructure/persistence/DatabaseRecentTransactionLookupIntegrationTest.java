package com.capitec.fraud.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.infrastructure.config.JpaAuditingConfiguration;
import com.capitec.fraud.infrastructure.persistence.mapper.TransactionEntityMapper;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.support.PostgresTestContainerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
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
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({
    DatabaseRecentTransactionLookup.class,
    DatabaseRecentTransactionLookupIntegrationTest.FixedClockConfiguration.class,
    JpaAuditingConfiguration.class,
    TransactionEntityMapper.class
})
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class DatabaseRecentTransactionLookupIntegrationTest
{

    private static final Instant AUDIT_TIME = Instant.parse("2026-06-07T10:00:00Z");
    private static final Instant CURRENT_TRANSACTION_TIME = Instant.parse("2026-06-07T08:10:00Z");
    private static final Duration TIME_WINDOW = Duration.parse("PT10M");

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = PostgresTestContainerFactory.create();

    @jakarta.annotation.Resource
    private DatabaseRecentTransactionLookup databaseRecentTransactionLookup;

    @jakarta.annotation.Resource
    private TransactionRepository transactionRepository;

    @jakarta.annotation.Resource
    private TransactionEntityMapper transactionEntityMapper;

    @jakarta.annotation.Resource
    private EntityManager entityManager;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
    }

    @Test
    void findsRecentCustomerOrAccountTransactionsAndExcludesCurrentTransaction()
    {
        var currentTransaction = sampleTransaction(
                "event-current",
                "tx-current",
                "customer-1",
                "account-1",
                CURRENT_TRANSACTION_TIME);
        saveTransaction(currentTransaction);
        saveTransaction(sampleTransaction(
                "event-customer-match",
                "tx-customer-match",
                "customer-1",
                "account-other",
                CURRENT_TRANSACTION_TIME.minus(Duration.parse("PT1M"))));
        saveTransaction(sampleTransaction(
                "event-account-match",
                "tx-account-match",
                "customer-other",
                "account-1",
                CURRENT_TRANSACTION_TIME.minus(Duration.parse("PT2M"))));
        saveTransaction(sampleTransaction(
                "event-old",
                "tx-old",
                "customer-1",
                "account-1",
                CURRENT_TRANSACTION_TIME.minus(Duration.parse("PT11M"))));
        saveTransaction(sampleTransaction(
                "event-unrelated",
                "tx-unrelated",
                "customer-other",
                "account-other",
                CURRENT_TRANSACTION_TIME.minus(Duration.parse("PT1M"))));
        entityManager.flush();
        entityManager.clear();

        var recentTransactions = databaseRecentTransactionLookup.recentTransactions(currentTransaction, TIME_WINDOW);

        assertThat(recentTransactions)
                .extracting(Transaction::transactionId)
                .containsExactly("tx-customer-match", "tx-account-match");
    }

    private void saveTransaction(Transaction transaction)
    {
        transactionRepository.save(transactionEntityMapper.toEntity(transaction));
    }

    private static Transaction sampleTransaction(
            String eventId,
            String transactionId,
            String customerId,
            String accountId,
            Instant transactionTime)
    {
        return new Transaction(
                eventId,
                transactionId,
                customerId,
                accountId,
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                transactionTime,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }

    @TestConfiguration
    static class FixedClockConfiguration
    {

        @Bean
        Clock fixedClock()
        {
            return Clock.fixed(AUDIT_TIME, ZoneOffset.UTC);
        }
    }
}
