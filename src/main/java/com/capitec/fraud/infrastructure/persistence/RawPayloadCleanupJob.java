package com.capitec.fraud.infrastructure.persistence;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "fraud.raw-payload.cleanup",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RawPayloadCleanupJob
{

    private static final Logger LOG = LoggerFactory.getLogger(RawPayloadCleanupJob.class);

    private final RawPayloadCleanupService cleanupService;
    private final Clock clock;

    public RawPayloadCleanupJob(
            RawPayloadCleanupService cleanupService,
            Clock clock)
    {
        this.cleanupService = cleanupService;
        this.clock = clock;
    }

    @Scheduled(cron = "${fraud.raw-payload.cleanup.cron}")
    public void clearExpiredPayloads()
    {
        clearExpiredPayloads(Instant.now(clock));
    }

    void clearExpiredPayloads(Instant cleanupTime)
    {
        var result = cleanupService.clearExpiredPayloads(cleanupTime);

        LOG.info(
                "event=raw_payload_cleanup_completed transactionPayloadsCleared={} "
                        + "processedEventPayloadsCleared={} totalPayloadsCleared={}",
                result.transactionPayloadsCleared(),
                result.processedEventPayloadsCleared(),
                result.totalPayloadsCleared());
    }
}
