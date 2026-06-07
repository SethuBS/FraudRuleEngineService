package com.capitec.fraud.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.infrastructure.config.FraudRuleCatalogConfiguration;
import com.capitec.fraud.infrastructure.config.JpaAuditingConfiguration;
import com.capitec.fraud.infrastructure.persistence.entity.FraudRuleEntity;
import com.capitec.fraud.infrastructure.persistence.repository.FraudRuleRepository;
import com.capitec.fraud.rules.FraudRule;
import com.capitec.fraud.rules.RuleMatch;
import com.capitec.fraud.rules.TransactionContext;
import com.capitec.fraud.support.PostgresTestContainerFactory;

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
    FraudRuleCatalogConfiguration.class,
    JpaAuditingConfiguration.class,
    RuleDefinitionSeeder.class,
    RuleDefinitionSeederIntegrationTest.FixedClockConfiguration.class,
    RuleDefinitionSeederIntegrationTest.RuleConfiguration.class
})
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "fraud.rule-catalog.enabled-by-default=true",
    "fraud.rule-catalog.seed-on-startup=false"
})
class RuleDefinitionSeederIntegrationTest
{

    private static final Instant AUDIT_TIME = Instant.parse("2026-06-07T10:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = PostgresTestContainerFactory.create();

    @jakarta.annotation.Resource
    private RuleDefinitionSeeder ruleDefinitionSeeder;

    @jakarta.annotation.Resource
    private FraudRuleRepository fraudRuleRepository;

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
    void insertsNewRuleDefinition()
    {
        var summary = ruleDefinitionSeeder.synchronizeRuleDefinitions();

        entityManager.flush();
        entityManager.clear();

        assertThat(summary.inserted()).isEqualTo(1);
        assertThat(summary.updated()).isZero();
        assertThat(summary.unchanged()).isZero();
        assertThat(fraudRuleRepository.findByCode(RuleConfiguration.RULE_CODE))
                .hasValueSatisfying(rule ->
                {
                    assertThat(rule.getName()).isEqualTo(RuleConfiguration.RULE_NAME);
                    assertThat(rule.getDescription()).isEqualTo(RuleConfiguration.RULE_DESCRIPTION);
                    assertThat(rule.getSeverity()).isEqualTo(RiskLevel.HIGH.name());
                    assertThat(rule.getScore()).isEqualTo(RuleConfiguration.RULE_SCORE.value());
                    assertThat(rule.isEnabled()).isTrue();
                });
    }

    @Test
    void updatesMetadataAndPreservesOperationalEnabledState()
    {
        fraudRuleRepository.saveAndFlush(new FraudRuleEntity(
                RuleConfiguration.RULE_CODE,
                "Old Name",
                "Old description",
                false,
                RiskLevel.LOW.name(),
                RiskScore.ZERO.value()));
        entityManager.clear();

        var summary = ruleDefinitionSeeder.synchronizeRuleDefinitions();

        entityManager.flush();
        entityManager.clear();

        assertThat(summary.inserted()).isZero();
        assertThat(summary.updated()).isEqualTo(1);
        assertThat(summary.unchanged()).isZero();
        assertThat(fraudRuleRepository.findByCode(RuleConfiguration.RULE_CODE))
                .hasValueSatisfying(rule ->
                {
                    assertThat(rule.getName()).isEqualTo(RuleConfiguration.RULE_NAME);
                    assertThat(rule.getDescription()).isEqualTo(RuleConfiguration.RULE_DESCRIPTION);
                    assertThat(rule.getSeverity()).isEqualTo(RiskLevel.HIGH.name());
                    assertThat(rule.getScore()).isEqualTo(RuleConfiguration.RULE_SCORE.value());
                    assertThat(rule.isEnabled()).isFalse();
                });
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

    @TestConfiguration
    static class RuleConfiguration
    {

        static final String RULE_CODE = "HIGH_AMOUNT";
        static final String RULE_NAME = "High Amount";
        static final String RULE_DESCRIPTION = "Amount exceeded configured threshold";
        static final RiskScore RULE_SCORE = RiskScore.of(55);

        @Bean
        FraudRule highAmountRule()
        {
            return new TestFraudRule(
                    RULE_CODE,
                    RULE_NAME,
                    RULE_DESCRIPTION,
                    RULE_SCORE,
                    RiskLevel.HIGH);
        }
    }

    private record TestFraudRule(
            String code,
            String name,
            String description,
            RiskScore defaultScore,
            RiskLevel severity) implements FraudRule
    {

        @Override
        public RuleMatch evaluate(TransactionContext context)
        {
            return RuleMatch.notMatched("Seeder test rule is not evaluated");
        }
    }
}
