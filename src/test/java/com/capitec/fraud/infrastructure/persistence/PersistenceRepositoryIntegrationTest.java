package com.capitec.fraud.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capitec.fraud.application.FraudAlertSearchQuery;
import com.capitec.fraud.application.FraudAlertView;
import com.capitec.fraud.application.FraudRetrievalService;
import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.RuleEvaluationResult;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.config.FraudIdempotencyConfiguration;
import com.capitec.fraud.infrastructure.config.FraudPersistenceConfiguration;
import com.capitec.fraud.infrastructure.config.JpaAuditingConfiguration;
import com.capitec.fraud.infrastructure.config.RandomAlertIdGenerator;
import com.capitec.fraud.infrastructure.persistence.entity.FraudRuleEntity;
import com.capitec.fraud.infrastructure.persistence.entity.ProcessedEventEntity;
import com.capitec.fraud.infrastructure.persistence.mapper.FraudAlertEntityMapper;
import com.capitec.fraud.infrastructure.persistence.mapper.RuleEvaluationEntityMapper;
import com.capitec.fraud.infrastructure.persistence.mapper.TransactionEntityMapper;
import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;
import com.capitec.fraud.infrastructure.persistence.repository.FraudRuleRepository;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.RuleEvaluationRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.support.PostgresIntegrationTest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    FraudIdempotencyConfiguration.class,
    FraudPersistenceConfiguration.class,
    FraudAlertEntityMapper.class,
    JpaAuditingConfiguration.class,
    PersistenceRepositoryIntegrationTest.FixedClockConfiguration.class,
    RandomAlertIdGenerator.class,
    RuleEvaluationEntityMapper.class,
    TransactionEntityMapper.class,
    DatabaseFraudRetrievalService.class,
    TransactionEvaluationPersistenceService.class
})
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "fraud.idempotency.completed-status=COMPLETED",
    "fraud.persistence.default-alert-status=OPEN"
})
class PersistenceRepositoryIntegrationTest extends PostgresIntegrationTest
{

    private static final Instant AUDIT_TIME = Instant.parse("2026-06-07T10:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T08:01:00Z");
    private static final String ALERT_FILTER_RULE_CODE = "ALERT_FILTER";
    private static final String ALERT_FILTER_RULE_NAME = "Alert Filter";
    private static final String ALERT_FILTER_RULE_DESCRIPTION = "Matched for alert retrieval filtering";
    private static final String ALERT_FILTER_RULE_SEVERITY = "HIGH";
    private static final int ALERT_FILTER_HIGH_SCORE = 55;
    private static final int ALERT_FILTER_CRITICAL_SCORE = 85;
    private static final int FIRST_PAGE = 0;
    private static final int SINGLE_ITEM_PAGE_SIZE = 1;
    private static final int ALERT_LIST_PAGE_SIZE = 10;
    private static final int FILTERED_ALERT_COUNT = 2;
    private static final int TOTAL_ALERT_COUNT = 3;
    private static final long FIRST_ALERT_OFFSET_SECONDS = 1L;
    private static final long SECOND_ALERT_OFFSET_SECONDS = 2L;
    private static final long THIRD_ALERT_OFFSET_SECONDS = 3L;

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = createPostgresContainer();

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry)
    {
        registerDatasourceProperties(registry, POSTGRESQL);
    }

    @jakarta.annotation.Resource
    private TransactionEvaluationPersistenceService persistenceService;

    @jakarta.annotation.Resource
    private FraudRetrievalService fraudRetrievalService;

    @jakarta.annotation.Resource
    private TransactionRepository transactionRepository;

    @jakarta.annotation.Resource
    private RuleEvaluationRepository ruleEvaluationRepository;

    @jakarta.annotation.Resource
    private FraudRuleRepository fraudRuleRepository;

    @jakarta.annotation.Resource
    private FraudAlertRepository fraudAlertRepository;

    @jakarta.annotation.Resource
    private ProcessedEventRepository processedEventRepository;

    @jakarta.annotation.Resource
    private TransactionEntityMapper transactionEntityMapper;

    @jakarta.annotation.Resource
    private EntityManager entityManager;

    @Test
    void persistsTransactionEvaluationsAndRetrievesFraudAlerts()
    {
        fraudRuleRepository.saveAll(List.of(
                new FraudRuleEntity("HIGH_AMOUNT", "High Amount", "Amount exceeded threshold", true, "HIGH", 55),
                new FraudRuleEntity("VELOCITY", "Velocity", "Velocity threshold was not met", true, "MEDIUM", 25)));

        var evaluation = TransactionEvaluation.from(
                sampleTransaction(),
                List.of(
                        RuleEvaluationResult.matched("HIGH_AMOUNT", "High Amount", 55, "Amount exceeded threshold", EVALUATED_AT),
                        RuleEvaluationResult.notMatched("VELOCITY", "Velocity", 25, "Velocity threshold was not met", EVALUATED_AT)),
                baselinePolicy(),
                EVALUATED_AT);

        var transactionEntity = persistenceService.persistEvaluation(evaluation);
        var alert = evaluation.toFraudAlert(UUID.fromString("b3ed20ca-bac6-45dc-a37e-d61001ee37ab")).orElseThrow();
        var alertEntity = persistenceService.persistAlert(alert);

        entityManager.flush();
        entityManager.clear();

        assertThat(transactionEntity.getId()).isNotNull();
        assertThat(alertEntity.getId()).isNotNull();
        assertThat(transactionRepository.findByTransactionId("tx-1")).isPresent();
        assertThat(persistenceService.findTransactionByTransactionId("tx-1")).isPresent();

        var ruleEvaluations = persistenceService.findRuleEvaluationsByTransactionId("tx-1");
        assertThat(ruleEvaluations)
                .hasSize(2)
                .extracting(ruleEvaluation -> ruleEvaluation.isMatched())
                .containsExactlyInAnyOrder(true, false);
        assertThat(ruleEvaluationRepository.findByTransactionTransactionIdAndMatchedOrderByCreatedAtAsc("tx-1", true))
                .singleElement()
                .satisfies(ruleEvaluation ->
                {
                    assertThat(ruleEvaluation.getRuleCode()).isEqualTo("HIGH_AMOUNT");
                    assertThat(ruleEvaluation.getFraudRule()).isNotNull();
                });

        var alerts = fraudAlertRepository.findByCustomerIdAndAccountIdAndRiskLevelAndCreatedAtBetweenOrderByCreatedAtDesc(
                "customer-1",
                "account-1",
                RiskLevel.HIGH,
                AUDIT_TIME.minusSeconds(1),
                AUDIT_TIME.plusSeconds(1));

        assertThat(alerts)
                .singleElement()
                .satisfies(storedAlert ->
                {
                    assertThat(storedAlert.getAlertId()).isEqualTo(alert.alertId());
                    assertThat(storedAlert.getDecision()).isEqualTo(FraudDecision.FLAGGED);
                    assertThat(storedAlert.getStatus()).isEqualTo("OPEN");
                    assertThat(storedAlert.getTransaction().getTransactionId()).isEqualTo("tx-1");
                });

        var pagedAlerts = fraudRetrievalService.findAlerts(new FraudAlertSearchQuery(
                "customer-1",
                "account-1",
                RiskLevel.HIGH,
                AUDIT_TIME.minusSeconds(1),
                AUDIT_TIME.plusSeconds(1),
                0,
                10));

        assertThat(pagedAlerts.totalElements()).isEqualTo(1);
        assertThat(pagedAlerts.content())
                .singleElement()
                .satisfies(storedAlert -> assertThat(storedAlert.alertId()).isEqualTo(alert.alertId()));
    }

    @Test
    void persistsProcessedEventsAndMapsTransactionsBackToDomain()
    {
        processedEventRepository.save(new ProcessedEventEntity(
                "event-1",
                "tx-1",
                "COMPLETED",
                null,
                AUDIT_TIME.plusSeconds(1)));

        var transactionEntity = transactionRepository.save(transactionEntityMapper.toEntity(sampleTransaction()));

        entityManager.flush();
        entityManager.clear();

        assertThat(processedEventRepository.existsByEventId("event-1")).isTrue();
        assertThat(processedEventRepository.findByEventId("event-1")).isPresent();

        var transaction = transactionEntityMapper.toDomain(transactionEntity);

        assertThat(transaction.transactionId()).isEqualTo("tx-1");
        assertThat(transaction.amount().amount()).isEqualByComparingTo("100.50");
        assertThat(transaction.amount().currencyCode()).isEqualTo("ZAR");
        assertThat(transaction.category().code()).isEqualTo("GROCERY");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void processedEventsEnforceUniqueEventIds()
    {
        processedEventRepository.saveAndFlush(new ProcessedEventEntity(
                "event-duplicate-constraint",
                "tx-duplicate-constraint-1",
                "COMPLETED",
                null,
                AUDIT_TIME.plusSeconds(FIRST_ALERT_OFFSET_SECONDS)));

        assertThatThrownBy(() -> processedEventRepository.saveAndFlush(new ProcessedEventEntity(
                "event-duplicate-constraint",
                "tx-duplicate-constraint-2",
                "COMPLETED",
                null,
                AUDIT_TIME.plusSeconds(SECOND_ALERT_OFFSET_SECONDS))))
                .hasRootCauseInstanceOf(SQLException.class);
    }

    @Test
    void filtersFraudAlertsByCustomerRiskLevelAndPaginates()
    {
        saveAlertFilterRule();
        persistAlertedEvaluation(
                "event-filter-high-a",
                "tx-filter-high-a",
                "customer-filter-1",
                "account-filter-1",
                ALERT_FILTER_HIGH_SCORE,
                EVALUATED_AT.plusSeconds(FIRST_ALERT_OFFSET_SECONDS));
        persistAlertedEvaluation(
                "event-filter-critical",
                "tx-filter-critical",
                "customer-filter-1",
                "account-filter-2",
                ALERT_FILTER_CRITICAL_SCORE,
                EVALUATED_AT.plusSeconds(SECOND_ALERT_OFFSET_SECONDS));
        persistAlertedEvaluation(
                "event-filter-high-b",
                "tx-filter-high-b",
                "customer-filter-2",
                "account-filter-3",
                ALERT_FILTER_HIGH_SCORE,
                EVALUATED_AT.plusSeconds(THIRD_ALERT_OFFSET_SECONDS));

        entityManager.flush();
        entityManager.clear();

        var customerAlerts = fraudRetrievalService.findAlerts(new FraudAlertSearchQuery(
                "customer-filter-1",
                null,
                null,
                null,
                null,
                FIRST_PAGE,
                ALERT_LIST_PAGE_SIZE));
        var highRiskAlerts = fraudRetrievalService.findAlerts(new FraudAlertSearchQuery(
                null,
                null,
                RiskLevel.HIGH,
                null,
                null,
                FIRST_PAGE,
                ALERT_LIST_PAGE_SIZE));
        var firstPage = fraudRetrievalService.findAlerts(new FraudAlertSearchQuery(
                null,
                null,
                null,
                null,
                null,
                FIRST_PAGE,
                SINGLE_ITEM_PAGE_SIZE));

        assertThat(customerAlerts.totalElements()).isEqualTo(FILTERED_ALERT_COUNT);
        assertThat(customerAlerts.content())
                .extracting(FraudAlertView::transactionId)
                .containsExactlyInAnyOrder("tx-filter-high-a", "tx-filter-critical");
        assertThat(highRiskAlerts.totalElements()).isEqualTo(FILTERED_ALERT_COUNT);
        assertThat(highRiskAlerts.content())
                .extracting(FraudAlertView::transactionId)
                .containsExactlyInAnyOrder("tx-filter-high-a", "tx-filter-high-b");
        assertThat(firstPage.content()).hasSize(SINGLE_ITEM_PAGE_SIZE);
        assertThat(firstPage.totalElements()).isEqualTo(TOTAL_ALERT_COUNT);
        assertThat(firstPage.totalPages()).isEqualTo(TOTAL_ALERT_COUNT);
        assertThat(firstPage.page()).isEqualTo(FIRST_PAGE);
        assertThat(firstPage.size()).isEqualTo(SINGLE_ITEM_PAGE_SIZE);
    }

    private static RiskPolicy baselinePolicy()
    {
        return new RiskPolicy(
                RiskScore.of(25),
                RiskScore.of(50),
                RiskScore.of(75),
                RiskScore.of(100),
                RiskLevel.MEDIUM,
                RiskLevel.HIGH);
    }

    private static Transaction sampleTransaction()
    {
        return sampleTransaction(
                "event-1",
                "tx-1",
                "customer-1",
                "account-1",
                Instant.parse("2026-06-07T08:00:00Z"));
    }

    private static Transaction sampleTransaction(
            String eventId,
            String transactionId,
            String customerId,
            String accountId,
            Instant transactionTimestamp)
    {
        return new Transaction(
                eventId,
                transactionId,
                customerId,
                accountId,
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                transactionTimestamp,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }

    private void saveAlertFilterRule()
    {
        fraudRuleRepository.save(new FraudRuleEntity(
                ALERT_FILTER_RULE_CODE,
                ALERT_FILTER_RULE_NAME,
                ALERT_FILTER_RULE_DESCRIPTION,
                true,
                ALERT_FILTER_RULE_SEVERITY,
                ALERT_FILTER_HIGH_SCORE));
    }

    private void persistAlertedEvaluation(
            String eventId,
            String transactionId,
            String customerId,
            String accountId,
            int score,
            Instant evaluatedAt)
    {
        var evaluation = TransactionEvaluation.from(
                sampleTransaction(
                        eventId,
                        transactionId,
                        customerId,
                        accountId,
                        evaluatedAt.minusSeconds(FIRST_ALERT_OFFSET_SECONDS)),
                List.of(RuleEvaluationResult.matched(
                        ALERT_FILTER_RULE_CODE,
                        ALERT_FILTER_RULE_NAME,
                        score,
                        ALERT_FILTER_RULE_DESCRIPTION,
                        evaluatedAt)),
                baselinePolicy(),
                evaluatedAt);
        persistenceService.persistEvaluation(evaluation);
        persistenceService.persistAlert(evaluation.toFraudAlert(alertId(transactionId)).orElseThrow());
    }

    private static UUID alertId(String transactionId)
    {
        return UUID.nameUUIDFromBytes(transactionId.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class FixedClockConfiguration
    {

        @Bean
        Clock fixedClock()
        {
            return Clock.fixed(AUDIT_TIME, ZoneOffset.UTC);
        }

        @Bean
        RiskPolicy riskPolicy()
        {
            return baselinePolicy();
        }
    }
}
