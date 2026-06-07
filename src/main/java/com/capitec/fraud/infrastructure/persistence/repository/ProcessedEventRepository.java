package com.capitec.fraud.infrastructure.persistence.repository;

import com.capitec.fraud.infrastructure.persistence.entity.ProcessedEventEntity;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long>
{

    Optional<ProcessedEventEntity> findByEventId(String eventId);

    Optional<ProcessedEventEntity> findFirstByTransactionIdOrderByCreatedAtAsc(String transactionId);

    boolean existsByEventId(String eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProcessedEventEntity processedEvent
            SET processedEvent.sanitizedRawPayload = null,
                processedEvent.rawPayloadExpiresAt = null,
                processedEvent.updatedAt = :cleanedAt
            WHERE processedEvent.rawPayloadExpiresAt < :expiresBefore
              AND processedEvent.sanitizedRawPayload IS NOT NULL
            """)
    int clearExpiredRawPayloads(
            @Param("expiresBefore") Instant expiresBefore,
            @Param("cleanedAt") Instant cleanedAt);
}
