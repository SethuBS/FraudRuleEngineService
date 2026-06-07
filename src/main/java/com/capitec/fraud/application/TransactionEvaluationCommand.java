package com.capitec.fraud.application;

import com.capitec.fraud.domain.Transaction;

import java.time.Instant;
import java.util.Objects;

public record TransactionEvaluationCommand(
        Transaction transaction,
        String sanitizedRawPayload,
        Instant rawPayloadExpiresAt)
{

    public TransactionEvaluationCommand
    {
        Objects.requireNonNull(transaction, "transaction is required");
    }

    public static TransactionEvaluationCommand withoutRawPayload(Transaction transaction)
    {
        return new TransactionEvaluationCommand(transaction, null, null);
    }
}
