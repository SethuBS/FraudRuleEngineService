package com.capitec.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class ForeignCountryTransactionRuleTest
{

    private static final String EXPECTED_COUNTRY = "ZA";
    private static final RiskScore DEFAULT_SCORE = RiskScore.of(45);
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");

    private final ForeignCountryTransactionRule rule = new ForeignCountryTransactionRule(
            EXPECTED_COUNTRY,
            DEFAULT_SCORE,
            RiskLevel.HIGH);

    @Test
    void localCountryDoesNotMatch()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("za")));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("Transaction country ZA")
                .contains("matches expected home country ZA");
    }

    @Test
    void foreignCountryMatches()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("US")));

        assertThat(result.matched()).isTrue();
        assertThat(result.explanation())
                .contains("Transaction country US")
                .contains("differs from expected home country ZA");
    }

    @Test
    void missingCountryDoesNotCrashAndDoesNotMatch()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction(null)));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("Transaction country is missing")
                .contains("expected home country is ZA");
    }

    @Test
    void exposesConfiguredMetadata()
    {
        assertThat(rule.code()).isEqualTo(ForeignCountryTransactionRule.RULE_CODE);
        assertThat(rule.name()).isEqualTo("Foreign Country Transaction");
        assertThat(rule.description()).contains("configured home country");
        assertThat(rule.defaultScore()).isEqualTo(DEFAULT_SCORE);
        assertThat(rule.severity()).isEqualTo(RiskLevel.HIGH);
    }

    private static Transaction sampleTransaction(String country)
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
                country,
                "device-1");
    }
}
