package com.capitec.fraud.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity extends AuditedEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = SchemaColumns.EVENT_ID_LENGTH)
    private String eventId;

    @Column(name = "transaction_id", length = SchemaColumns.TRANSACTION_ID_LENGTH)
    private String transactionId;

    @Column(name = "processing_status", nullable = false, length = SchemaColumns.STATUS_LENGTH)
    private String processingStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sanitized_raw_payload", columnDefinition = SchemaColumns.JSONB_COLUMN)
    private String sanitizedRawPayload;

    @Column(name = "raw_payload_expires_at")
    private Instant rawPayloadExpiresAt;

    protected ProcessedEventEntity()
    {
    }

    public ProcessedEventEntity(
            String eventId,
            String transactionId,
            String processingStatus,
            String sanitizedRawPayload,
            Instant rawPayloadExpiresAt)
    {
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.processingStatus = processingStatus;
        this.sanitizedRawPayload = sanitizedRawPayload;
        this.rawPayloadExpiresAt = rawPayloadExpiresAt;
    }

    public Long getId()
    {
        return id;
    }

    public String getEventId()
    {
        return eventId;
    }

    public String getTransactionId()
    {
        return transactionId;
    }

    public String getProcessingStatus()
    {
        return processingStatus;
    }

    public String getSanitizedRawPayload()
    {
        return sanitizedRawPayload;
    }

    public Instant getRawPayloadExpiresAt()
    {
        return rawPayloadExpiresAt;
    }
}
