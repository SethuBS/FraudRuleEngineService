package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.RuleEvaluationRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "fraud.idempotency.completed-status=COMPLETED"
})
class TransactionEvaluationIdempotencyIntegrationTest
{

    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String POSTGRES_IMAGE = System.getProperty(
            "test.postgres.image",
            System.getenv().getOrDefault("TEST_POSTGRES_IMAGE", "postgres:16-alpine"));

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(
            DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
            .withStartupTimeout(Duration.ofMinutes(2));

    @jakarta.annotation.Resource
    private TransactionEvaluationService transactionEvaluationService;

    @jakarta.annotation.Resource
    private CountingTransactionEvaluationEngine transactionEvaluationEngine;

    @jakarta.annotation.Resource
    private ProcessedEventRepository processedEventRepository;

    @jakarta.annotation.Resource
    private TransactionRepository transactionRepository;

    @jakarta.annotation.Resource
    private RuleEvaluationRepository ruleEvaluationRepository;

    @jakarta.annotation.Resource
    private FraudAlertRepository fraudAlertRepository;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
    }

    @BeforeEach
    void resetDatabase()
    {
        fraudAlertRepository.deleteAll();
        ruleEvaluationRepository.deleteAll();
        transactionRepository.deleteAll();
        processedEventRepository.deleteAll();
        transactionEvaluationEngine.reset();
    }

    @Test
    void sameEventSubmittedTwiceSequentiallyReturnsPreviousEvaluation()
    {
        var transaction = sampleTransaction("event-1", "tx-1");

        var firstEvaluation = transactionEvaluationService.evaluate(transaction);
        var secondEvaluation = transactionEvaluationService.evaluate(transaction);

        assertThat(firstEvaluation.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(secondEvaluation.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(secondEvaluation.transaction().eventId()).isEqualTo("event-1");
        assertThat(transactionEvaluationEngine.invocationCount()).isEqualTo(1);
        assertThat(processedEventRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(ruleEvaluationRepository.count()).isZero();
        assertThat(fraudAlertRepository.count()).isZero();
        assertThat(processedEventRepository.findByEventId("event-1"))
                .hasValueSatisfying(processedEvent ->
                {
                    assertThat(processedEvent.getProcessingStatus()).isEqualTo(COMPLETED_STATUS);
                    assertThat(processedEvent.getEvaluatedAt()).isEqualTo(EVALUATED_AT);
                });
    }

    @Test
    void sameTransactionIdSubmittedTwiceSequentiallyReturnsPreviousEvaluation()
    {
        var originalTransaction = sampleTransaction("event-1", "tx-1");
        var duplicateTransaction = sampleTransaction("event-2", "tx-1");

        transactionEvaluationService.evaluate(originalTransaction);
        var duplicateEvaluation = transactionEvaluationService.evaluate(duplicateTransaction);

        assertThat(duplicateEvaluation.transaction().eventId()).isEqualTo("event-1");
        assertThat(duplicateEvaluation.transaction().transactionId()).isEqualTo("tx-1");
        assertThat(transactionEvaluationEngine.invocationCount()).isEqualTo(1);
        assertThat(processedEventRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    private static Transaction sampleTransaction(String eventId, String transactionId)
    {
        return new Transaction(
                eventId,
                transactionId,
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                TRANSACTION_TIME,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }

    @TestConfiguration
    static class CountingEngineConfiguration
    {

        @Bean
        @Primary
        Clock fixedClock()
        {
            return Clock.fixed(EVALUATED_AT, ZoneOffset.UTC);
        }

        @Bean
        @Primary
        CountingTransactionEvaluationEngine countingTransactionEvaluationEngine(
                RiskPolicy riskPolicy,
                Clock fixedClock)
        {
            return new CountingTransactionEvaluationEngine(riskPolicy, fixedClock);
        }
    }

    static class CountingTransactionEvaluationEngine implements TransactionEvaluationEngine
    {

        private final RiskPolicy riskPolicy;
        private final Clock clock;
        private final AtomicInteger invocationCount = new AtomicInteger();

        CountingTransactionEvaluationEngine(RiskPolicy riskPolicy, Clock clock)
        {
            this.riskPolicy = riskPolicy;
            this.clock = clock;
        }

        @Override
        public TransactionEvaluation evaluate(Transaction transaction)
        {
            invocationCount.incrementAndGet();

            return TransactionEvaluation.from(transaction, List.of(), riskPolicy, Instant.now(clock));
        }

        int invocationCount()
        {
            return invocationCount.get();
        }

        void reset()
        {
            invocationCount.set(0);
        }
    }
}
