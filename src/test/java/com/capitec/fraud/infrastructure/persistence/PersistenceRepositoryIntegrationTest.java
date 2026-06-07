package com.capitec.fraud.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.application.FraudAlertSearchQuery;
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
import com.capitec.fraud.support.PostgresTestContainerFactory;

import java.math.BigDecimal;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
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
class PersistenceRepositoryIntegrationTest
{

    private static final Instant AUDIT_TIME = Instant.parse("2026-06-07T10:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T08:01:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = PostgresTestContainerFactory.create();

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
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
        return new Transaction(
                "event-1",
                "tx-1",
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                Instant.parse("2026-06-07T08:00:00Z"),
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

        @Bean
        RiskPolicy riskPolicy()
        {
            return baselinePolicy();
        }
    }
}
