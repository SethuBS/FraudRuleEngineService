package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RuleEvaluationResult;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.RuleEvaluationRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.support.PostgresIntegrationTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "fraud.idempotency.completed-status=COMPLETED"
})
class TransactionEvaluationIdempotencyIntegrationTest extends PostgresIntegrationTest
{

    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final Instant RAW_PAYLOAD_EXPIRES_AT = Instant.parse("2026-06-14T09:00:00Z");
    private static final Duration CONCURRENT_TEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String CUSTOMER_ID = "customer-1";
    private static final String ACCOUNT_ID = "account-1";
    private static final String CURRENCY = "ZAR";
    private static final String MERCHANT_CATEGORY = "grocery";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String MERCHANT_NAME = "Corner Shop";
    private static final String CHANNEL = "mobile";
    private static final String COUNTRY = "za";
    private static final String DEVICE_ID = "device-1";
    private static final String SAMPLE_AMOUNT = "100.50";
    private static final String SAME_EVENT_ID = "event-1";
    private static final String SAME_TRANSACTION_DUPLICATE_EVENT_ID = "event-2";
    private static final String SAME_TRANSACTION_ID = "tx-1";
    private static final String ALERT_EVENT_ID = "event-alert";
    private static final String ALERT_TRANSACTION_ID = "tx-alert";
    private static final String ROLLBACK_EVENT_ID = "event-rollback";
    private static final String ROLLBACK_TRANSACTION_ID = "tx-rollback";
    private static final String CONCURRENT_EVENT_ID = "event-concurrent";
    private static final String CONCURRENT_TRANSACTION_ID = "tx-concurrent";
    private static final String SANITIZED_RAW_PAYLOAD = "{\"eventId\":\"event-1\"}";
    private static final String HIGH_AMOUNT_RULE_CODE = "HIGH_AMOUNT";
    private static final String HIGH_AMOUNT_RULE_NAME = "High Amount";
    private static final String HIGH_AMOUNT_RULE_EXPLANATION = "Amount exceeded configured threshold";
    private static final int HIGH_AMOUNT_RULE_SCORE = 55;
    private static final String DUPLICATE_RULE_CODE = "DUPLICATE_RULE";
    private static final String DUPLICATE_RULE_NAME = "Duplicate Rule";
    private static final String DUPLICATE_RULE_EXPLANATION = "Duplicate rule code for rollback verification";
    private static final int DUPLICATE_RULE_SCORE = 10;
    private static final int SINGLE_ENGINE_INVOCATION_COUNT = 1;
    private static final int SINGLE_RECORD_LIST_SIZE = 1;
    private static final int CONCURRENT_REQUEST_COUNT = 2;
    private static final long NO_RECORD_COUNT = 0L;
    private static final long SINGLE_RECORD_COUNT = 1L;

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = createPostgresContainer();

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
        registerDatasourceProperties(registry, POSTGRESQL);
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
    void failedEvaluationRollsBackPartialWrites()
    {
        var transaction = sampleTransaction(ROLLBACK_EVENT_ID, ROLLBACK_TRANSACTION_ID);
        transactionEvaluationEngine.useRuleResults(List.of(
                RuleEvaluationResult.matched(
                        DUPLICATE_RULE_CODE,
                        DUPLICATE_RULE_NAME,
                        DUPLICATE_RULE_SCORE,
                        DUPLICATE_RULE_EXPLANATION,
                        EVALUATED_AT),
                RuleEvaluationResult.notMatched(
                        DUPLICATE_RULE_CODE,
                        DUPLICATE_RULE_NAME,
                        DUPLICATE_RULE_SCORE,
                        DUPLICATE_RULE_EXPLANATION,
                        EVALUATED_AT)));

        assertThatThrownBy(() -> transactionEvaluationService.evaluate(transaction))
                .isInstanceOf(TransactionEvaluationException.class)
                .hasMessage("Transaction evaluation could not be completed safely");

        assertThat(processedEventRepository.count()).isEqualTo(NO_RECORD_COUNT);
        assertThat(transactionRepository.count()).isEqualTo(NO_RECORD_COUNT);
        assertThat(ruleEvaluationRepository.count()).isEqualTo(NO_RECORD_COUNT);
        assertThat(fraudAlertRepository.count()).isEqualTo(NO_RECORD_COUNT);
    }

    @Test
    void validTransactionPersistsEvaluationResultsAndAlertAtomically()
    {
        var transaction = sampleTransaction(ALERT_EVENT_ID, ALERT_TRANSACTION_ID);
        transactionEvaluationEngine.useRuleResults(alertingRuleResults());

        var evaluation = transactionEvaluationService.evaluate(new TransactionEvaluationCommand(
                transaction,
                SANITIZED_RAW_PAYLOAD,
                RAW_PAYLOAD_EXPIRES_AT));

        assertThat(evaluation.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertSingleAlertedEvaluation(ALERT_TRANSACTION_ID);
        assertThat(ruleEvaluationRepository.findByTransactionTransactionIdOrderByCreatedAtAsc(ALERT_TRANSACTION_ID))
                .singleElement()
                .satisfies(ruleEvaluation ->
                {
                    assertThat(ruleEvaluation.getRuleCode()).isEqualTo(HIGH_AMOUNT_RULE_CODE);
                    assertThat(ruleEvaluation.getRuleName()).isEqualTo(HIGH_AMOUNT_RULE_NAME);
                    assertThat(ruleEvaluation.isMatched()).isTrue();
                    assertThat(ruleEvaluation.getScoreContribution()).isEqualTo(HIGH_AMOUNT_RULE_SCORE);
                    assertThat(ruleEvaluation.getExplanation()).isEqualTo(HIGH_AMOUNT_RULE_EXPLANATION);
                    assertThat(ruleEvaluation.getEvaluatedAt()).isEqualTo(EVALUATED_AT);
                });
        assertThat(fraudAlertRepository.findByTransactionTransactionIdOrderByCreatedAtDesc(ALERT_TRANSACTION_ID))
                .singleElement()
                .satisfies(alert ->
                {
                    assertThat(alert.getCustomerId()).isEqualTo(CUSTOMER_ID);
                    assertThat(alert.getAccountId()).isEqualTo(ACCOUNT_ID);
                    assertThat(alert.getDecision()).isEqualTo(FraudDecision.FLAGGED);
                    assertThat(alert.getRiskScore()).isEqualTo(HIGH_AMOUNT_RULE_SCORE);
                    assertThat(alert.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
                });
    }

    @Test
    void sameEventSubmittedTwiceSequentiallyReturnsPreviousEvaluation()
    {
        var transaction = sampleTransaction(SAME_EVENT_ID, SAME_TRANSACTION_ID);
        transactionEvaluationEngine.useRuleResults(alertingRuleResults());

        var firstEvaluation = transactionEvaluationService.evaluate(new TransactionEvaluationCommand(
                transaction,
                SANITIZED_RAW_PAYLOAD,
                RAW_PAYLOAD_EXPIRES_AT));
        var secondEvaluation = transactionEvaluationService.evaluate(transaction);

        assertDeterministicEvaluation(secondEvaluation, firstEvaluation);
        assertThat(secondEvaluation.transaction().eventId()).isEqualTo(SAME_EVENT_ID);
        assertThat(transactionEvaluationEngine.invocationCount()).isEqualTo(SINGLE_ENGINE_INVOCATION_COUNT);
        assertSingleAlertedEvaluation(SAME_TRANSACTION_ID);
        assertThat(processedEventRepository.findByEventId(SAME_EVENT_ID))
                .hasValueSatisfying(processedEvent ->
                {
                    assertThat(processedEvent.getProcessingStatus()).isEqualTo(COMPLETED_STATUS);
                    assertThat(processedEvent.getEvaluatedAt()).isEqualTo(EVALUATED_AT);
                    assertThat(processedEvent.getSanitizedRawPayload())
                            .contains("\"eventId\"")
                            .contains("\"event-1\"");
                    assertThat(processedEvent.getRawPayloadExpiresAt()).isEqualTo(RAW_PAYLOAD_EXPIRES_AT);
                });
        assertThat(transactionRepository.findByTransactionId(SAME_TRANSACTION_ID))
                .hasValueSatisfying(storedTransaction ->
                {
                    assertThat(storedTransaction.getSanitizedRawPayload())
                            .contains("\"eventId\"")
                            .contains("\"event-1\"");
                    assertThat(storedTransaction.getRawPayloadExpiresAt()).isEqualTo(RAW_PAYLOAD_EXPIRES_AT);
                });
    }

    @Test
    void sameTransactionIdSubmittedTwiceSequentiallyReturnsPreviousEvaluation()
    {
        var originalTransaction = sampleTransaction(SAME_EVENT_ID, SAME_TRANSACTION_ID);
        var duplicateTransaction = sampleTransaction(SAME_TRANSACTION_DUPLICATE_EVENT_ID, SAME_TRANSACTION_ID);
        transactionEvaluationEngine.useRuleResults(alertingRuleResults());

        var firstEvaluation = transactionEvaluationService.evaluate(originalTransaction);
        var duplicateEvaluation = transactionEvaluationService.evaluate(duplicateTransaction);

        assertDeterministicEvaluation(duplicateEvaluation, firstEvaluation);
        assertThat(duplicateEvaluation.transaction().eventId()).isEqualTo(SAME_EVENT_ID);
        assertThat(duplicateEvaluation.transaction().transactionId()).isEqualTo(SAME_TRANSACTION_ID);
        assertThat(transactionEvaluationEngine.invocationCount()).isEqualTo(SINGLE_ENGINE_INVOCATION_COUNT);
        assertSingleAlertedEvaluation(SAME_TRANSACTION_ID);
        assertThat(processedEventRepository.findByEventId(SAME_TRANSACTION_DUPLICATE_EVENT_ID)).isEmpty();
    }

    @Test
    void sameEventSubmittedConcurrentlyReturnsOneStoredAlertedEvaluation()
            throws Exception
    {
        var transaction = sampleTransaction(CONCURRENT_EVENT_ID, CONCURRENT_TRANSACTION_ID);
        var command = new TransactionEvaluationCommand(
                transaction,
                sanitizedPayload(CONCURRENT_EVENT_ID),
                RAW_PAYLOAD_EXPIRES_AT);
        var startGate = new CountDownLatch(1);
        var executorService = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT);
        transactionEvaluationEngine.useRuleResults(alertingRuleResults());
        transactionEvaluationEngine.blockUntilEvaluations(CONCURRENT_REQUEST_COUNT);

        try
        {
            var firstEvaluation = executorService.submit(() -> evaluateAfterStart(startGate, command));
            var secondEvaluation = executorService.submit(() -> evaluateAfterStart(startGate, command));

            startGate.countDown();
            assertThat(transactionEvaluationEngine.awaitBlockedEvaluations(CONCURRENT_TEST_TIMEOUT)).isTrue();
            transactionEvaluationEngine.releaseBlockedEvaluations();

            var firstResult = completedEvaluation(firstEvaluation);
            var secondResult = completedEvaluation(secondEvaluation);

            assertDeterministicEvaluation(secondResult, firstResult);
            assertThat(transactionEvaluationEngine.invocationCount()).isEqualTo(CONCURRENT_REQUEST_COUNT);
            assertSingleAlertedEvaluation(CONCURRENT_TRANSACTION_ID);
            assertThat(processedEventRepository.findByEventId(CONCURRENT_EVENT_ID)).isPresent();
        }
        finally
        {
            transactionEvaluationEngine.releaseBlockedEvaluations();
            executorService.shutdownNow();
        }
    }

    private TransactionEvaluation evaluateAfterStart(
            CountDownLatch startGate,
            TransactionEvaluationCommand command)
    {
        awaitOrFail(startGate, CONCURRENT_TEST_TIMEOUT, "concurrent duplicate start gate");

        return transactionEvaluationService.evaluate(command);
    }

    private static TransactionEvaluation completedEvaluation(Future<TransactionEvaluation> future)
            throws Exception
    {
        return future.get(CONCURRENT_TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void assertSingleAlertedEvaluation(String transactionId)
    {
        assertThat(processedEventRepository.count()).isEqualTo(SINGLE_RECORD_COUNT);
        assertThat(transactionRepository.count()).isEqualTo(SINGLE_RECORD_COUNT);
        assertThat(ruleEvaluationRepository.count()).isEqualTo(SINGLE_RECORD_COUNT);
        assertThat(fraudAlertRepository.count()).isEqualTo(SINGLE_RECORD_COUNT);
        assertThat(ruleEvaluationRepository.findByTransactionTransactionIdOrderByCreatedAtAsc(transactionId))
                .hasSize(SINGLE_RECORD_LIST_SIZE);
        assertThat(fraudAlertRepository.findByTransactionTransactionIdOrderByCreatedAtDesc(transactionId))
                .hasSize(SINGLE_RECORD_LIST_SIZE);
    }

    private static void assertDeterministicEvaluation(
            TransactionEvaluation actual,
            TransactionEvaluation expected)
    {
        assertThat(actual)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    private static List<RuleEvaluationResult> alertingRuleResults()
    {
        return List.of(RuleEvaluationResult.matched(
                HIGH_AMOUNT_RULE_CODE,
                HIGH_AMOUNT_RULE_NAME,
                HIGH_AMOUNT_RULE_SCORE,
                HIGH_AMOUNT_RULE_EXPLANATION,
                EVALUATED_AT));
    }

    private static String sanitizedPayload(String eventId)
    {
        return "{\"eventId\":\"" + eventId + "\"}";
    }

    private static void awaitOrFail(
            CountDownLatch latch,
            Duration timeout,
            String operation)
    {
        try
        {
            if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS))
            {
                throw new AssertionError(operation + " timed out");
            }
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new AssertionError(operation + " was interrupted", ex);
        }
    }

    private static Transaction sampleTransaction(String eventId, String transactionId)
    {
        return new Transaction(
                eventId,
                transactionId,
                CUSTOMER_ID,
                ACCOUNT_ID,
                Money.of(new BigDecimal(SAMPLE_AMOUNT), CURRENCY),
                TransactionCategory.of(MERCHANT_CATEGORY),
                TRANSACTION_TIME,
                MERCHANT_ID,
                MERCHANT_NAME,
                CHANNEL,
                COUNTRY,
                DEVICE_ID);
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
        private List<RuleEvaluationResult> ruleResults = List.of();
        private CountDownLatch blockedEvaluationEntries = new CountDownLatch(0);
        private CountDownLatch blockedEvaluationRelease = new CountDownLatch(0);

        CountingTransactionEvaluationEngine(RiskPolicy riskPolicy, Clock clock)
        {
            this.riskPolicy = riskPolicy;
            this.clock = clock;
        }

        @Override
        public TransactionEvaluation evaluate(Transaction transaction)
        {
            invocationCount.incrementAndGet();
            waitIfBlockingEnabled();

            return TransactionEvaluation.from(transaction, ruleResults, riskPolicy, Instant.now(clock));
        }

        int invocationCount()
        {
            return invocationCount.get();
        }

        void useRuleResults(List<RuleEvaluationResult> ruleResults)
        {
            this.ruleResults = List.copyOf(ruleResults);
        }

        void blockUntilEvaluations(int expectedEvaluationCount)
        {
            blockedEvaluationEntries = new CountDownLatch(expectedEvaluationCount);
            blockedEvaluationRelease = new CountDownLatch(1);
        }

        boolean awaitBlockedEvaluations(Duration timeout)
        {
            try
            {
                return blockedEvaluationEntries.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        void releaseBlockedEvaluations()
        {
            blockedEvaluationRelease.countDown();
        }

        void reset()
        {
            invocationCount.set(0);
            ruleResults = List.of();
            blockedEvaluationEntries = new CountDownLatch(0);
            blockedEvaluationRelease = new CountDownLatch(0);
        }

        private void waitIfBlockingEnabled()
        {
            if (blockedEvaluationRelease.getCount() == 0)
            {
                return;
            }

            blockedEvaluationEntries.countDown();
            awaitOrFail(blockedEvaluationRelease, CONCURRENT_TEST_TIMEOUT, "blocked duplicate evaluation release");
        }
    }
}
