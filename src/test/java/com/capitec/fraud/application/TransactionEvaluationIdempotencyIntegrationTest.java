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
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String SANITIZED_RAW_PAYLOAD = "{\"eventId\":\"event-1\"}";
    private static final String HIGH_AMOUNT_RULE_CODE = "HIGH_AMOUNT";
    private static final String HIGH_AMOUNT_RULE_NAME = "High Amount";
    private static final String HIGH_AMOUNT_RULE_EXPLANATION = "Amount exceeded configured threshold";
    private static final int HIGH_AMOUNT_RULE_SCORE = 55;
    private static final String DUPLICATE_RULE_CODE = "DUPLICATE_RULE";
    private static final String DUPLICATE_RULE_NAME = "Duplicate Rule";
    private static final String DUPLICATE_RULE_EXPLANATION = "Duplicate rule code for rollback verification";
    private static final int DUPLICATE_RULE_SCORE = 10;

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
        var transaction = sampleTransaction("event-rollback", "tx-rollback");
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

        assertThat(processedEventRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
        assertThat(ruleEvaluationRepository.count()).isZero();
        assertThat(fraudAlertRepository.count()).isZero();
    }

    @Test
    void validTransactionPersistsEvaluationResultsAndAlertAtomically()
    {
        var transaction = sampleTransaction("event-alert", "tx-alert");
        transactionEvaluationEngine.useRuleResults(List.of(RuleEvaluationResult.matched(
                HIGH_AMOUNT_RULE_CODE,
                HIGH_AMOUNT_RULE_NAME,
                HIGH_AMOUNT_RULE_SCORE,
                HIGH_AMOUNT_RULE_EXPLANATION,
                EVALUATED_AT)));

        var evaluation = transactionEvaluationService.evaluate(new TransactionEvaluationCommand(
                transaction,
                SANITIZED_RAW_PAYLOAD,
                RAW_PAYLOAD_EXPIRES_AT));

        assertThat(evaluation.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(processedEventRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(ruleEvaluationRepository.count()).isEqualTo(1);
        assertThat(fraudAlertRepository.count()).isEqualTo(1);
        assertThat(ruleEvaluationRepository.findByTransactionTransactionIdOrderByCreatedAtAsc("tx-alert"))
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
        assertThat(fraudAlertRepository.findByTransactionTransactionIdOrderByCreatedAtDesc("tx-alert"))
                .singleElement()
                .satisfies(alert ->
                {
                    assertThat(alert.getCustomerId()).isEqualTo("customer-1");
                    assertThat(alert.getAccountId()).isEqualTo("account-1");
                    assertThat(alert.getDecision()).isEqualTo(FraudDecision.FLAGGED);
                    assertThat(alert.getRiskScore()).isEqualTo(HIGH_AMOUNT_RULE_SCORE);
                    assertThat(alert.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
                });
    }

    @Test
    void sameEventSubmittedTwiceSequentiallyReturnsPreviousEvaluation()
    {
        var transaction = sampleTransaction("event-1", "tx-1");

        var firstEvaluation = transactionEvaluationService.evaluate(new TransactionEvaluationCommand(
                transaction,
                SANITIZED_RAW_PAYLOAD,
                RAW_PAYLOAD_EXPIRES_AT));
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
                    assertThat(processedEvent.getSanitizedRawPayload())
                            .contains("\"eventId\"")
                            .contains("\"event-1\"");
                    assertThat(processedEvent.getRawPayloadExpiresAt()).isEqualTo(RAW_PAYLOAD_EXPIRES_AT);
                });
        assertThat(transactionRepository.findByTransactionId("tx-1"))
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
        private List<RuleEvaluationResult> ruleResults = List.of();

        CountingTransactionEvaluationEngine(RiskPolicy riskPolicy, Clock clock)
        {
            this.riskPolicy = riskPolicy;
            this.clock = clock;
        }

        @Override
        public TransactionEvaluation evaluate(Transaction transaction)
        {
            invocationCount.incrementAndGet();

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

        void reset()
        {
            invocationCount.set(0);
            ruleResults = List.of();
        }
    }
}
