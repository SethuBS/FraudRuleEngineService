package com.capitec.fraud.rules;

import com.capitec.fraud.domain.FraudAlert;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

import java.util.List;
import java.util.Objects;

public record TransactionContext(
        Transaction transaction,
        List<TransactionEvaluation> historicalEvaluations,
        List<FraudAlert> historicalAlerts)
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
    }

    public static TransactionContext current(Transaction transaction)
    {
        return new TransactionContext(transaction, List.of(), List.of());
    }
}
