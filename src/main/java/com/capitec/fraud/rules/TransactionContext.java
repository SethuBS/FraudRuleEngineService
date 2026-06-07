package com.capitec.fraud.rules;

import com.capitec.fraud.domain.FraudAlert;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record TransactionContext(
        Transaction transaction,
        List<TransactionEvaluation> historicalEvaluations,
        List<FraudAlert> historicalAlerts,
        RecentTransactionLookup recentTransactionLookup)
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
    }

    public static TransactionContext current(Transaction transaction)
    {
        return current(transaction, RecentTransactionLookup.empty());
    }

    public static TransactionContext current(
            Transaction transaction,
            RecentTransactionLookup recentTransactionLookup)
    {
        return new TransactionContext(transaction, List.of(), List.of(), recentTransactionLookup);
    }

    public List<Transaction> recentTransactions(Duration timeWindow)
    {
        return recentTransactionLookup.recentTransactions(transaction, timeWindow);
    }
}
