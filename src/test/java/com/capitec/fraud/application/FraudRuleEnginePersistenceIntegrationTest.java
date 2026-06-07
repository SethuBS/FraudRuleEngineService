package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.RuleEvaluationRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.rules.ForeignCountryTransactionRule;
import com.capitec.fraud.rules.FraudRule;
import com.capitec.fraud.rules.HighValueTransactionRule;
import com.capitec.fraud.rules.RiskyMerchantCategoryRule;
import com.capitec.fraud.rules.RuleMatch;
import com.capitec.fraud.rules.TransactionContext;
import com.capitec.fraud.rules.VelocityTransactionRule;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

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
    "fraud.rule-catalog.enabled-by-default=true",
    "fraud.rule-catalog.seed-on-startup=true",
    "fraud.rules.foreign-country-transaction.expected-country=ZA",
    "fraud.rules.foreign-country-transaction.default-score=45",
    "fraud.rules.foreign-country-transaction.severity=HIGH",
    "fraud.rules.high-value-transaction.threshold-amount=1000.00",
    "fraud.rules.high-value-transaction.default-score=40",
    "fraud.rules.high-value-transaction.severity=HIGH",
    "fraud.rules.risky-merchant-category.risk-categories=GAMBLING,CRYPTO,JEWELRY",
    "fraud.rules.risky-merchant-category.default-score=30",
    "fraud.rules.risky-merchant-category.severity=MEDIUM",
    "fraud.rules.velocity-transaction.transaction-count-threshold=5",
    "fraud.rules.velocity-transaction.time-window-minutes=10",
    "fraud.rules.velocity-transaction.default-score=35",
    "fraud.rules.velocity-transaction.severity=HIGH"
})
class FraudRuleEnginePersistenceIntegrationTest
{

    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final String MATCHED_RULE_CODE = "ALWAYS_MATCH";
    private static final String MATCHED_RULE_NAME = "Always Match";
    private static final String MATCHED_RULE_DESCRIPTION = "Matches integration transactions";
    private static final String MATCHED_RULE_EXPLANATION = "Transaction matched integration rule";
    private static final RiskScore MATCHED_RULE_SCORE = RiskScore.of(55);
    private static final String FOREIGN_COUNTRY_RULE_EXPLANATION =
            "Transaction country ZA matches expected home country ZA";
    private static final RiskScore FOREIGN_COUNTRY_RULE_SCORE = RiskScore.of(45);
    private static final String HIGH_VALUE_RULE_EXPLANATION =
            "Transaction amount 100.50 ZAR is within high-value threshold 1000.00 ZAR";
    private static final RiskScore HIGH_VALUE_RULE_SCORE = RiskScore.of(40);
    private static final String UNMATCHED_RULE_CODE = "NEVER_MATCH";
    private static final String UNMATCHED_RULE_NAME = "Never Match";
    private static final String UNMATCHED_RULE_DESCRIPTION = "Does not match integration transactions";
    private static final String UNMATCHED_RULE_EXPLANATION = "Transaction did not match integration rule";
    private static final RiskScore UNMATCHED_RULE_SCORE = RiskScore.of(10);
    private static final String RISKY_MERCHANT_CATEGORY_RULE_EXPLANATION =
            "Merchant category GROCERY is not configured as risky";
    private static final RiskScore RISKY_MERCHANT_CATEGORY_RULE_SCORE = RiskScore.of(30);
    private static final String VELOCITY_RULE_EXPLANATION =
            "Observed 1 transactions within 10 minutes, within velocity threshold 5";
    private static final RiskScore VELOCITY_RULE_SCORE = RiskScore.of(35);
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
    }

    @Test
    void persistsMatchedAndUnmatchedRuleResults()
    {
        var evaluation = transactionEvaluationService.evaluate(sampleTransaction());

        assertThat(evaluation.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(evaluation.riskScore()).isEqualTo(MATCHED_RULE_SCORE);
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(evaluation.ruleResults())
                .extracting(
                        result -> result.ruleCode(),
                        result -> result.matched(),
                        result -> result.scoreContribution(),
                        result -> result.explanation())
                .containsExactly(
                        tuple(MATCHED_RULE_CODE, true, MATCHED_RULE_SCORE, MATCHED_RULE_EXPLANATION),
                        tuple(
                                ForeignCountryTransactionRule.RULE_CODE,
                                false,
                                FOREIGN_COUNTRY_RULE_SCORE,
                                FOREIGN_COUNTRY_RULE_EXPLANATION),
                        tuple(
                                HighValueTransactionRule.RULE_CODE,
                                false,
                                HIGH_VALUE_RULE_SCORE,
                                HIGH_VALUE_RULE_EXPLANATION),
                        tuple(UNMATCHED_RULE_CODE, false, UNMATCHED_RULE_SCORE, UNMATCHED_RULE_EXPLANATION),
                        tuple(
                                RiskyMerchantCategoryRule.RULE_CODE,
                                false,
                                RISKY_MERCHANT_CATEGORY_RULE_SCORE,
                                RISKY_MERCHANT_CATEGORY_RULE_EXPLANATION),
                        tuple(
                                VelocityTransactionRule.RULE_CODE,
                                false,
                                VELOCITY_RULE_SCORE,
                                VELOCITY_RULE_EXPLANATION));
        assertThat(evaluation.matchedRules())
                .singleElement()
                .satisfies(rule -> assertThat(rule.ruleCode()).isEqualTo(MATCHED_RULE_CODE));
        assertThat(processedEventRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(fraudAlertRepository.count()).isEqualTo(1);
        assertThat(ruleEvaluationRepository.findByTransactionTransactionIdOrderByCreatedAtAsc("tx-1"))
                .extracting(
                        ruleEvaluation -> ruleEvaluation.getRuleCode(),
                        ruleEvaluation -> ruleEvaluation.isMatched(),
                        ruleEvaluation -> RiskScore.of(ruleEvaluation.getScoreContribution()),
                        ruleEvaluation -> ruleEvaluation.getExplanation())
                .containsExactlyInAnyOrder(
                        tuple(MATCHED_RULE_CODE, true, MATCHED_RULE_SCORE, MATCHED_RULE_EXPLANATION),
                        tuple(
                                ForeignCountryTransactionRule.RULE_CODE,
                                false,
                                FOREIGN_COUNTRY_RULE_SCORE,
                                FOREIGN_COUNTRY_RULE_EXPLANATION),
                        tuple(
                                HighValueTransactionRule.RULE_CODE,
                                false,
                                HIGH_VALUE_RULE_SCORE,
                                HIGH_VALUE_RULE_EXPLANATION),
                        tuple(UNMATCHED_RULE_CODE, false, UNMATCHED_RULE_SCORE, UNMATCHED_RULE_EXPLANATION),
                        tuple(
                                RiskyMerchantCategoryRule.RULE_CODE,
                                false,
                                RISKY_MERCHANT_CATEGORY_RULE_SCORE,
                                RISKY_MERCHANT_CATEGORY_RULE_EXPLANATION),
                        tuple(
                                VelocityTransactionRule.RULE_CODE,
                                false,
                                VELOCITY_RULE_SCORE,
                                VELOCITY_RULE_EXPLANATION));
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
                TRANSACTION_TIME,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }

    @TestConfiguration
    static class RuleEngineConfiguration
    {

        @Bean
        @Primary
        Clock fixedClock()
        {
            return Clock.fixed(EVALUATED_AT, ZoneOffset.UTC);
        }

        @Bean
        FraudRule alwaysMatchRule()
        {
            return new TestFraudRule(
                    MATCHED_RULE_CODE,
                    MATCHED_RULE_NAME,
                    MATCHED_RULE_DESCRIPTION,
                    MATCHED_RULE_SCORE,
                    RiskLevel.HIGH,
                    RuleMatch.matched(MATCHED_RULE_EXPLANATION));
        }

        @Bean
        FraudRule neverMatchRule()
        {
            return new TestFraudRule(
                    UNMATCHED_RULE_CODE,
                    UNMATCHED_RULE_NAME,
                    UNMATCHED_RULE_DESCRIPTION,
                    UNMATCHED_RULE_SCORE,
                    RiskLevel.LOW,
                    RuleMatch.notMatched(UNMATCHED_RULE_EXPLANATION));
        }
    }

    private record TestFraudRule(
            String code,
            String name,
            String description,
            RiskScore defaultScore,
            RiskLevel severity,
            RuleMatch match) implements FraudRule
    {

        @Override
        public RuleMatch evaluate(TransactionContext context)
        {
            return match;
        }
    }
}
