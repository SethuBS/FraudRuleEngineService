package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.rules.FraudRule;
import com.capitec.fraud.rules.HistoricalAverageAmountLookup;
import com.capitec.fraud.rules.RecentTransactionLookup;
import com.capitec.fraud.rules.RuleMatch;
import com.capitec.fraud.rules.TransactionContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class FraudRuleEngineTest
{

    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final String MATCHED_RULE_CODE = "ALWAYS_MATCH";
    private static final String MATCHED_RULE_NAME = "Always Match";
    private static final String MATCHED_RULE_DESCRIPTION = "Matches test transactions";
    private static final String MATCHED_RULE_EXPLANATION = "Transaction matched test rule";
    private static final RiskScore MATCHED_RULE_SCORE = RiskScore.of(55);
    private static final String UNMATCHED_RULE_CODE = "NEVER_MATCH";
    private static final String UNMATCHED_RULE_NAME = "Never Match";
    private static final String UNMATCHED_RULE_DESCRIPTION = "Does not match test transactions";
    private static final String UNMATCHED_RULE_EXPLANATION = "Transaction did not match test rule";
    private static final RiskScore UNMATCHED_RULE_SCORE = RiskScore.of(10);
    private static final String DISABLED_RULE_CODE = "DISABLED_RULE";

    @Test
    void evaluatesEnabledRulesDeterministicallyAndIncludesUnmatchedResults()
    {
        var matchedRule = new TestFraudRule(
                MATCHED_RULE_CODE,
                MATCHED_RULE_NAME,
                MATCHED_RULE_DESCRIPTION,
                MATCHED_RULE_SCORE,
                RiskLevel.HIGH,
                RuleMatch.matched(MATCHED_RULE_EXPLANATION));
        var unmatchedRule = new TestFraudRule(
                UNMATCHED_RULE_CODE,
                UNMATCHED_RULE_NAME,
                UNMATCHED_RULE_DESCRIPTION,
                UNMATCHED_RULE_SCORE,
                RiskLevel.LOW,
                RuleMatch.notMatched(UNMATCHED_RULE_EXPLANATION));
        var disabledRule = new TestFraudRule(
                DISABLED_RULE_CODE,
                "Disabled Rule",
                "Disabled rule is not executed",
                RiskScore.ZERO,
                RiskLevel.LOW,
                RuleMatch.matched("Disabled result"));
        var engine = new FraudRuleEngine(
                List.of(unmatchedRule, disabledRule, matchedRule),
                ruleCode -> !DISABLED_RULE_CODE.equals(ruleCode),
                RecentTransactionLookup.empty(),
                HistoricalAverageAmountLookup.empty(),
                baselinePolicy(),
                Clock.fixed(EVALUATED_AT, ZoneOffset.UTC));

        var evaluation = engine.evaluate(sampleTransaction());

        assertThat(evaluation.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(evaluation.riskScore()).isEqualTo(MATCHED_RULE_SCORE);
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(evaluation.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(evaluation.ruleResults())
                .extracting(
                        result -> result.ruleCode(),
                        result -> result.ruleName(),
                        result -> result.matched(),
                        result -> result.scoreContribution(),
                        result -> result.explanation(),
                        result -> result.evaluatedAt())
                .containsExactly(
                        tuple(
                                MATCHED_RULE_CODE,
                                MATCHED_RULE_NAME,
                                true,
                                MATCHED_RULE_SCORE,
                                MATCHED_RULE_EXPLANATION,
                                EVALUATED_AT),
                        tuple(
                                UNMATCHED_RULE_CODE,
                                UNMATCHED_RULE_NAME,
                                false,
                                UNMATCHED_RULE_SCORE,
                                UNMATCHED_RULE_EXPLANATION,
                                EVALUATED_AT));
        assertThat(matchedRule.invocationCount()).isEqualTo(1);
        assertThat(unmatchedRule.invocationCount()).isEqualTo(1);
        assertThat(disabledRule.invocationCount()).isZero();
    }

    private static RiskPolicy baselinePolicy()
    {
        return new RiskPolicy(
                RiskScore.of(25),
                RiskScore.of(50),
                RiskScore.of(75),
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
                TRANSACTION_TIME,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }

    private static class TestFraudRule implements FraudRule
    {

        private final String code;
        private final String name;
        private final String description;
        private final RiskScore defaultScore;
        private final RiskLevel severity;
        private final RuleMatch match;
        private int invocationCount;

        TestFraudRule(
                String code,
                String name,
                String description,
                RiskScore defaultScore,
                RiskLevel severity,
                RuleMatch match)
        {
            this.code = code;
            this.name = name;
            this.description = description;
            this.defaultScore = defaultScore;
            this.severity = severity;
            this.match = match;
        }

        @Override
        public String code()
        {
            return code;
        }

        @Override
        public String name()
        {
            return name;
        }

        @Override
        public String description()
        {
            return description;
        }

        @Override
        public RiskScore defaultScore()
        {
            return defaultScore;
        }

        @Override
        public RiskLevel severity()
        {
            return severity;
        }

        @Override
        public RuleMatch evaluate(TransactionContext context)
        {
            assertThat(context.transaction().transactionId()).isEqualTo("tx-1");
            assertThat(context.historicalEvaluations()).isEmpty();
            assertThat(context.historicalAlerts()).isEmpty();
            invocationCount++;

            return match;
        }

        int invocationCount()
        {
            return invocationCount;
        }
    }
}
