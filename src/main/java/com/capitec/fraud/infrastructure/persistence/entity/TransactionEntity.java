package com.capitec.fraud.infrastructure.persistence.entity;

import java.math.BigDecimal;
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
@Table(name = "transactions")
public class TransactionEntity extends AuditedEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = SchemaColumns.EVENT_ID_LENGTH)
    private String eventId;

    @Column(name = "transaction_id", nullable = false, unique = true, length = SchemaColumns.TRANSACTION_ID_LENGTH)
    private String transactionId;

    @Column(name = "customer_id", nullable = false, length = SchemaColumns.CUSTOMER_ID_LENGTH)
    private String customerId;

    @Column(name = "account_id", nullable = false, length = SchemaColumns.ACCOUNT_ID_LENGTH)
    private String accountId;

    @Column(
            name = "amount",
            nullable = false,
            precision = SchemaColumns.MONEY_PRECISION,
            scale = SchemaColumns.MONEY_SCALE)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = SchemaColumns.CURRENCY_LENGTH)
    private String currency;

    @Column(name = "transaction_timestamp", nullable = false)
    private Instant transactionTimestamp;

    @Column(name = "merchant_category", nullable = false, length = SchemaColumns.MERCHANT_CATEGORY_LENGTH)
    private String merchantCategory;

    @Column(name = "country", length = SchemaColumns.COUNTRY_LENGTH)
    private String country;

    @Column(name = "channel", length = SchemaColumns.CHANNEL_LENGTH)
    private String channel;

    @Column(name = "merchant_id", length = SchemaColumns.MERCHANT_ID_LENGTH)
    private String merchantId;

    @Column(name = "merchant_name", length = SchemaColumns.MERCHANT_NAME_LENGTH)
    private String merchantName;

    @Column(name = "device_id", length = SchemaColumns.DEVICE_ID_LENGTH)
    private String deviceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sanitized_raw_payload", columnDefinition = SchemaColumns.JSONB_COLUMN)
    private String sanitizedRawPayload;

    @Column(name = "raw_payload_expires_at")
    private Instant rawPayloadExpiresAt;

    protected TransactionEntity()
    {
    }

    public TransactionEntity(
            String eventId,
            String transactionId,
            String customerId,
            String accountId,
            BigDecimal amount,
            String currency,
            Instant transactionTimestamp,
            String merchantCategory,
            String country,
            String channel,
            String merchantId,
            String merchantName,
            String deviceId,
            String sanitizedRawPayload,
            Instant rawPayloadExpiresAt)
    {
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.transactionTimestamp = transactionTimestamp;
        this.merchantCategory = merchantCategory;
        this.country = country;
        this.channel = channel;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.deviceId = deviceId;
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

    public String getCustomerId()
    {
        return customerId;
    }

    public String getAccountId()
    {
        return accountId;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public String getCurrency()
    {
        return currency;
    }

    public Instant getTransactionTimestamp()
    {
        return transactionTimestamp;
    }

    public String getMerchantCategory()
    {
        return merchantCategory;
    }

    public String getCountry()
    {
        return country;
    }

    public String getChannel()
    {
        return channel;
    }

    public String getMerchantId()
    {
        return merchantId;
    }

    public String getMerchantName()
    {
        return merchantName;
    }

    public String getDeviceId()
    {
        return deviceId;
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
