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
    DatabaseHistoricalAverageAmountLookup.class,
    DatabaseHistoricalAverageAmountLookupIntegrationTest.FixedClockConfiguration.class,
    JpaAuditingConfiguration.class,
    TransactionEntityMapper.class
})
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class DatabaseHistoricalAverageAmountLookupIntegrationTest
{

    private static final Instant AUDIT_TIME = Instant.parse("2026-06-07T10:00:00Z");
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:10:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = PostgresTestContainerFactory.create();

    @jakarta.annotation.Resource
    private DatabaseHistoricalAverageAmountLookup databaseHistoricalAverageAmountLookup;

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
    void averagesHistoricalCustomerOrAccountTransactionsAndExcludesCurrentTransaction()
    {
        var currentTransaction = sampleTransaction(
                "event-current",
                "tx-current",
                "customer-1",
                "account-1",
                "900.00",
                "ZAR");
        saveTransaction(currentTransaction);
        saveTransaction(sampleTransaction(
                "event-customer-match",
                "tx-customer-match",
                "customer-1",
                "account-other",
                "100.00",
                "ZAR"));
        saveTransaction(sampleTransaction(
                "event-account-match",
                "tx-account-match",
                "customer-other",
                "account-1",
                "200.00",
                "ZAR"));
        saveTransaction(sampleTransaction(
                "event-unrelated",
                "tx-unrelated",
                "customer-other",
                "account-other",
                "1000.00",
                "ZAR"));
        saveTransaction(sampleTransaction(
                "event-currency",
                "tx-currency",
                "customer-1",
                "account-1",
                "1000.00",
                "USD"));
        entityManager.flush();
        entityManager.clear();

        var averageAmount = databaseHistoricalAverageAmountLookup.averageAmount(currentTransaction);

        assertThat(averageAmount).isPresent();
        assertThat(averageAmount.orElseThrow().amount()).isEqualByComparingTo("150.00");
        assertThat(averageAmount.orElseThrow().currencyCode()).isEqualTo("ZAR");
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
            String amount,
            String currency)
    {
        return new Transaction(
                eventId,
                transactionId,
                customerId,
                accountId,
                Money.of(new BigDecimal(amount), currency),
                TransactionCategory.of("grocery"),
                TRANSACTION_TIME,
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
