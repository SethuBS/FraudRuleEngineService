package com.capitec.fraud.rules;

import com.capitec.fraud.domain.FraudAlert;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TransactionContext(
        Transaction transaction,
        List<TransactionEvaluation> historicalEvaluations,
        List<FraudAlert> historicalAlerts,
        RecentTransactionLookup recentTransactionLookup,
        HistoricalAverageAmountLookup historicalAverageAmountLookup)
{

    public TransactionContext
    {
        Objects.requireNonNull(transaction, "transaction is required");
        historicalEvaluations = List.copyOf(Objects.requireNonNull(
                historicalEvaluations,
                "historicalEvaluations is required"));
        historicalAlerts = List.copyOf(Objects.requireNonNull(
                historicalAlerts,
                "historicalAlerts is required"));
        recentTransactionLookup = Objects.requireNonNull(
                recentTransactionLookup,
                "recentTransactionLookup is required");
        historicalAverageAmountLookup = Objects.requireNonNull(
                historicalAverageAmountLookup,
                "historicalAverageAmountLookup is required");
    }

    public static TransactionContext current(Transaction transaction)
    {
        return current(
                transaction,
                RecentTransactionLookup.empty(),
                HistoricalAverageAmountLookup.empty());
    }

    public static TransactionContext current(
            Transaction transaction,
            RecentTransactionLookup recentTransactionLookup)
    {
        return current(transaction, recentTransactionLookup, HistoricalAverageAmountLookup.empty());
    }

    public static TransactionContext current(
            Transaction transaction,
            HistoricalAverageAmountLookup historicalAverageAmountLookup)
    {
        return current(transaction, RecentTransactionLookup.empty(), historicalAverageAmountLookup);
    }

    public static TransactionContext current(
            Transaction transaction,
            RecentTransactionLookup recentTransactionLookup,
            HistoricalAverageAmountLookup historicalAverageAmountLookup)
    {
        return new TransactionContext(
                transaction,
                List.of(),
                List.of(),
                recentTransactionLookup,
                historicalAverageAmountLookup);
    }

    public List<Transaction> recentTransactions(Duration timeWindow)
    {
        return recentTransactionLookup.recentTransactions(transaction, timeWindow);
    }

    public Optional<Money> historicalAverageAmount()
    {
        return historicalAverageAmountLookup.averageAmount(transaction);
    }
}
