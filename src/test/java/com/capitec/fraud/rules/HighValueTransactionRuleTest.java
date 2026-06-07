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

class HighValueTransactionRuleTest
{

    private static final BigDecimal THRESHOLD_AMOUNT = new BigDecimal("1000.00");
    private static final RiskScore DEFAULT_SCORE = RiskScore.of(55);
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");

    private final HighValueTransactionRule rule = new HighValueTransactionRule(
            THRESHOLD_AMOUNT,
            DEFAULT_SCORE,
            RiskLevel.HIGH);

    @Test
    void belowThresholdDoesNotMatch()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("999.99")));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("999.99 ZAR")
                .contains("within high-value threshold")
                .contains("1000.00 ZAR");
    }

    @Test
    void equalThresholdDoesNotMatch()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("1000.00")));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("1000.00 ZAR")
                .contains("within high-value threshold");
    }

    @Test
    void aboveThresholdMatches()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("1000.01")));

        assertThat(result.matched()).isTrue();
        assertThat(result.explanation())
                .contains("1000.01 ZAR")
                .contains("exceeds high-value threshold")
                .contains("1000.00 ZAR");
    }

    @Test
    void exposesConfiguredMetadata()
    {
        assertThat(rule.code()).isEqualTo(HighValueTransactionRule.RULE_CODE);
        assertThat(rule.name()).isEqualTo("High Value Transaction");
        assertThat(rule.description()).contains("configured high-value threshold");
        assertThat(rule.defaultScore()).isEqualTo(DEFAULT_SCORE);
        assertThat(rule.severity()).isEqualTo(RiskLevel.HIGH);
    }

    private static Transaction sampleTransaction(String amount)
    {
        return new Transaction(
                "event-1",
                "tx-1",
                "customer-1",
                "account-1",
                Money.of(new BigDecimal(amount), "ZAR"),
                TransactionCategory.of("grocery"),
                TRANSACTION_TIME,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }
}
