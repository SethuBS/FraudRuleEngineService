package com.capitec.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class UnusualAmountRuleTest
{

    private static final BigDecimal MULTIPLIER_THRESHOLD = new BigDecimal("3.00");
    private static final RiskScore DEFAULT_SCORE = RiskScore.of(45);
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");

    private final UnusualAmountRule rule = new UnusualAmountRule(
            MULTIPLIER_THRESHOLD,
            DEFAULT_SCORE,
            RiskLevel.HIGH);

    @Test
    void noHistoryDoesNotMatch()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("300.01")));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("No historical average amount baseline")
                .contains("ZAR");
    }

    @Test
    void normalAmountDoesNotMatch()
    {
        var result = rule.evaluate(contextWithAverage(sampleTransaction("300.00"), "100.00"));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("Current amount 300.00 ZAR")
                .contains("within 3.00x historical average amount 100.00 ZAR");
    }

    @Test
    void unusualAmountMatches()
    {
        var result = rule.evaluate(contextWithAverage(sampleTransaction("300.01"), "100.00"));

        assertThat(result.matched()).isTrue();
        assertThat(result.explanation())
                .contains("Current amount 300.01 ZAR")
                .contains("exceeds 3.00x historical average amount 100.00 ZAR");
    }

    @Test
    void exposesConfiguredMetadata()
    {
        assertThat(rule.code()).isEqualTo(UnusualAmountRule.RULE_CODE);
        assertThat(rule.name()).isEqualTo("Unusual Amount");
        assertThat(rule.description()).contains("historical average");
        assertThat(rule.defaultScore()).isEqualTo(DEFAULT_SCORE);
        assertThat(rule.severity()).isEqualTo(RiskLevel.HIGH);
    }

    private static TransactionContext contextWithAverage(
            Transaction transaction,
            String averageAmount)
    {
        return TransactionContext.current(
                transaction,
                currentTransaction -> Optional.of(Money.of(new BigDecimal(averageAmount), currentTransaction.amount().currencyCode())));
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
