package com.capitec.fraud.infrastructure.persistence;

import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RawPayloadCleanupService
{

    private final TransactionRepository transactionRepository;
    private final ProcessedEventRepository processedEventRepository;

    public RawPayloadCleanupService(
            TransactionRepository transactionRepository,
            ProcessedEventRepository processedEventRepository)
    {
        this.transactionRepository = transactionRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public RawPayloadCleanupResult clearExpiredPayloads(Instant cleanupTime)
    {
        var transactionPayloadsCleared = transactionRepository.clearExpiredRawPayloads(cleanupTime, cleanupTime);
        var processedEventPayloadsCleared = processedEventRepository.clearExpiredRawPayloads(cleanupTime, cleanupTime);

        return new RawPayloadCleanupResult(transactionPayloadsCleared, processedEventPayloadsCleared);
    }
}
