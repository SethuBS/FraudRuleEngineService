package com.capitec.fraud.infrastructure.persistence;

import com.capitec.fraud.infrastructure.config.FraudRuleCatalogProperties;
import com.capitec.fraud.infrastructure.persistence.entity.FraudRuleEntity;
import com.capitec.fraud.infrastructure.persistence.repository.FraudRuleRepository;
import com.capitec.fraud.rules.FraudRule;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RuleDefinitionSeeder implements ApplicationRunner
{

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleDefinitionSeeder.class);

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudRuleCatalogProperties properties;
    private final List<FraudRule> fraudRules;

    public RuleDefinitionSeeder(
            FraudRuleRepository fraudRuleRepository,
            FraudRuleCatalogProperties properties,
            List<FraudRule> fraudRules)
    {
        this.fraudRuleRepository = fraudRuleRepository;
        this.properties = properties;
        this.fraudRules = List.copyOf(fraudRules);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args)
    {
        if (properties.seedOnStartup())
        {
            synchronizeRuleDefinitions();
        }
    }

    @Transactional
    public RuleCatalogSynchronizationSummary synchronizeRuleDefinitions()
    {
        var summary = RuleCatalogSynchronizationSummary.empty();

        for (var rule : sortedRules())
        {
            summary = synchronizeRule(rule, summary);
        }

        LOGGER.info(
                "Synchronized fraud rule catalog: inserted={}, updated={}, unchanged={}, total={}",
                summary.inserted(),
                summary.updated(),
                summary.unchanged(),
                summary.total());

        return summary;
    }

    private List<FraudRule> sortedRules()
    {
        return fraudRules.stream()
                .sorted(Comparator.comparing(FraudRule::code))
                .toList();
    }

    private RuleCatalogSynchronizationSummary synchronizeRule(
            FraudRule rule,
            RuleCatalogSynchronizationSummary summary)
    {
        return fraudRuleRepository.findByCode(rule.code())
                .map(existingRule -> updateExistingRule(existingRule, rule, summary))
                .orElseGet(() -> insertNewRule(rule, summary));
    }

    private RuleCatalogSynchronizationSummary updateExistingRule(
            FraudRuleEntity existingRule,
            FraudRule rule,
            RuleCatalogSynchronizationSummary summary)
    {
        var severity = rule.severity().name();
        var score = rule.defaultScore().value();

        if (existingRule.metadataMatches(rule.name(), rule.description(), severity, score))
        {
            return summary.withUnchangedRule();
        }

        existingRule.updateMetadata(rule.name(), rule.description(), severity, score);

        return summary.withUpdatedRule();
    }

    private RuleCatalogSynchronizationSummary insertNewRule(
            FraudRule rule,
            RuleCatalogSynchronizationSummary summary)
    {
        fraudRuleRepository.save(new FraudRuleEntity(
                rule.code(),
                rule.name(),
                rule.description(),
                properties.enabledByDefault(),
                rule.severity().name(),
                rule.defaultScore().value()));

        return summary.withInsertedRule();
    }

    public record RuleCatalogSynchronizationSummary(int inserted, int updated, int unchanged)
    {

        private static final int EMPTY_COUNT = 0;
        private static final int COUNTER_INCREMENT = 1;

        static RuleCatalogSynchronizationSummary empty()
        {
            return new RuleCatalogSynchronizationSummary(EMPTY_COUNT, EMPTY_COUNT, EMPTY_COUNT);
        }

        int total()
        {
            return inserted + updated + unchanged;
        }

        RuleCatalogSynchronizationSummary withInsertedRule()
        {
            return new RuleCatalogSynchronizationSummary(inserted + COUNTER_INCREMENT, updated, unchanged);
        }

        RuleCatalogSynchronizationSummary withUpdatedRule()
        {
            return new RuleCatalogSynchronizationSummary(inserted, updated + COUNTER_INCREMENT, unchanged);
        }

        RuleCatalogSynchronizationSummary withUnchangedRule()
        {
            return new RuleCatalogSynchronizationSummary(inserted, updated, unchanged + COUNTER_INCREMENT);
        }
    }
}
