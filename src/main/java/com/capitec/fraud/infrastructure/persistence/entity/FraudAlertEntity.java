package com.capitec.fraud.infrastructure.persistence.entity;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlertEntity extends AuditedEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, unique = true)
    private UUID alertId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id", nullable = false)
    private TransactionEntity transaction;

    @Column(name = "customer_id", nullable = false, length = SchemaColumns.CUSTOMER_ID_LENGTH)
    private String customerId;

    @Column(name = "account_id", nullable = false, length = SchemaColumns.ACCOUNT_ID_LENGTH)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = SchemaColumns.SCORE_CLASSIFICATION_LENGTH)
    private FraudDecision decision;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = SchemaColumns.SCORE_CLASSIFICATION_LENGTH)
    private RiskLevel riskLevel;

    @Column(name = "status", nullable = false, length = SchemaColumns.STATUS_LENGTH)
    private String status;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected FraudAlertEntity()
    {
    }

    public FraudAlertEntity(
            UUID alertId,
            TransactionEntity transaction,
            String customerId,
            String accountId,
            FraudDecision decision,
            int riskScore,
            RiskLevel riskLevel,
            String status)
    {
        this.alertId = alertId;
        this.transaction = transaction;
        this.customerId = customerId;
        this.accountId = accountId;
        this.decision = decision;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.status = status;
    }

    public Long getId()
    {
        return id;
    }

    public UUID getAlertId()
    {
        return alertId;
    }

    public TransactionEntity getTransaction()
    {
        return transaction;
    }

    public String getCustomerId()
    {
        return customerId;
    }

    public String getAccountId()
    {
        return accountId;
    }

    public FraudDecision getDecision()
    {
        return decision;
    }

    public int getRiskScore()
    {
        return riskScore;
    }

    public RiskLevel getRiskLevel()
    {
        return riskLevel;
    }

    public String getStatus()
    {
        return status;
    }

    public Instant getClosedAt()
    {
        return closedAt;
    }
}
