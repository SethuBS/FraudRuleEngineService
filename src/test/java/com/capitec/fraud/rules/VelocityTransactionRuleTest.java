package com.capitec.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class VelocityTransactionRuleTest
{

    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");
    private static final Duration TIME_WINDOW = Duration.parse("PT10M");
    private static final int TRANSACTION_COUNT_THRESHOLD = 3;
    private static final RiskScore DEFAULT_SCORE = RiskScore.of(35);

    private final VelocityTransactionRule rule = new VelocityTransactionRule(
            TRANSACTION_COUNT_THRESHOLD,
            TIME_WINDOW,
            DEFAULT_SCORE,
            RiskLevel.HIGH);

    @Test
    void noHistoricalTransactionsDoesNotMatchAndCountsCurrentTransaction()
    {
        var currentTransaction = sampleTransaction("event-current", "tx-current", TRANSACTION_TIME);
        var result = rule.evaluate(contextWithHistory(currentTransaction, List.of()));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("Observed 1 transactions")
                .contains("within 10 minutes")
                .contains("within velocity threshold 3");
    }

    @Test
    void normalVelocityDoesNotMatch()
    {
        var currentTransaction = sampleTransaction("event-current", "tx-current", TRANSACTION_TIME);
        var context = contextWithHistory(
                currentTransaction,
                List.of(sampleTransaction("event-previous-1", "tx-previous-1", TRANSACTION_TIME.minus(Duration.parse("PT1M")))));

        var result = rule.evaluate(context);

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("Observed 2 transactions")
                .contains("within 10 minutes")
                .contains("threshold 3");
    }

    @Test
    void equalThresholdDoesNotMatch()
    {
        var currentTransaction = sampleTransaction("event-current", "tx-current", TRANSACTION_TIME);
        var context = contextWithHistory(
                currentTransaction,
                List.of(
                        sampleTransaction("event-previous-1", "tx-previous-1", TRANSACTION_TIME.minus(Duration.parse("PT1M"))),
                        sampleTransaction("event-previous-2", "tx-previous-2", TRANSACTION_TIME.minus(Duration.parse("PT2M")))));

        var result = rule.evaluate(context);

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("Observed 3 transactions")
                .contains("within velocity threshold 3");
    }

    @Test
    void highVelocityMatches()
    {
        var currentTransaction = sampleTransaction("event-current", "tx-current", TRANSACTION_TIME);
        var context = contextWithHistory(
                currentTransaction,
                List.of(
                        sampleTransaction("event-previous-1", "tx-previous-1", TRANSACTION_TIME.minus(Duration.parse("PT1M"))),
                        sampleTransaction("event-previous-2", "tx-previous-2", TRANSACTION_TIME.minus(Duration.parse("PT2M"))),
                        sampleTransaction("event-previous-3", "tx-previous-3", TRANSACTION_TIME.minus(Duration.parse("PT3M")))));

        var result = rule.evaluate(context);

        assertThat(result.matched()).isTrue();
        assertThat(result.explanation())
                .contains("Observed 4 transactions")
                .contains("exceeding velocity threshold 3");
    }

    @Test
    void excludesCurrentTransactionFromHistoricalLookup()
    {
        var currentTransaction = sampleTransaction("event-current", "tx-current", TRANSACTION_TIME);
        var context = contextWithHistory(
                currentTransaction,
                List.of(
                        currentTransaction,
                        sampleTransaction("event-previous-1", "tx-previous-1", TRANSACTION_TIME.minus(Duration.parse("PT1M"))),
                        sampleTransaction("event-previous-2", "tx-previous-2", TRANSACTION_TIME.minus(Duration.parse("PT2M")))));

        var result = rule.evaluate(context);

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("Observed 3 transactions")
                .contains("within velocity threshold 3");
    }

    @Test
    void passesConfiguredTimeWindowToHistoryLookup()
    {
        var requestedWindow = new AtomicReference<Duration>();
        var currentTransaction = sampleTransaction("event-current", "tx-current", TRANSACTION_TIME);
        var context = TransactionContext.current(currentTransaction, (transaction, timeWindow) ->
        {
            requestedWindow.set(timeWindow);

            return List.of();
        });

        rule.evaluate(context);

        assertThat(requestedWindow).hasValue(TIME_WINDOW);
    }

    @Test
    void exposesConfiguredMetadata()
    {
        assertThat(rule.code()).isEqualTo(VelocityTransactionRule.RULE_CODE);
        assertThat(rule.name()).isEqualTo("Velocity Transaction");
        assertThat(rule.description()).contains("configured time window");
        assertThat(rule.defaultScore()).isEqualTo(DEFAULT_SCORE);
        assertThat(rule.severity()).isEqualTo(RiskLevel.HIGH);
    }

    private static TransactionContext contextWithHistory(
            Transaction currentTransaction,
            List<Transaction> recentTransactions)
    {
        return TransactionContext.current(currentTransaction, (transaction, timeWindow) -> recentTransactions);
    }

    private static Transaction sampleTransaction(
            String eventId,
            String transactionId,
            Instant transactionTime)
    {
        return new Transaction(
                eventId,
                transactionId,
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                transactionTime,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }
}
