package com.capitec.fraud.api.dto;

import com.capitec.fraud.application.FraudAlertView;
import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record FraudAlertResponse(
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

    public static FraudAlertResponse from(FraudAlertView alert)
    {
        return new FraudAlertResponse(
                alert.alertId(),
                alert.transactionId(),
                alert.customerId(),
                alert.accountId(),
                alert.decision(),
                alert.riskScore(),
                alert.riskLevel(),
                alert.status(),
                alert.createdAt(),
                alert.updatedAt(),
                alert.closedAt());
    }
}
