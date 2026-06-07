package com.capitec.fraud.infrastructure.persistence;

public record RawPayloadCleanupResult(
        int transactionPayloadsCleared,
        int processedEventPayloadsCleared)
{

    public int totalPayloadsCleared()
    {
        return Math.addExact(transactionPayloadsCleared, processedEventPayloadsCleared);
    }
}
