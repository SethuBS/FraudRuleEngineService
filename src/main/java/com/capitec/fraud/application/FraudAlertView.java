package com.capitec.fraud.application;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record FraudAlertView(
        UUID alertId,
        String transactionId,
        String customerId,
        String accountId,
        FraudDecision decision,
        int riskScore,
        RiskLevel riskLevel,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt)
{
}
