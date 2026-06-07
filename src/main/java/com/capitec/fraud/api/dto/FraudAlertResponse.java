package com.capitec.fraud.api.dto;

import com.capitec.fraud.application.FraudAlertView;
import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stored fraud alert opened for an alertable fraud decision.")
public record FraudAlertResponse(
        @Schema(description = "Unique alert id.")
        UUID alertId,
        @Schema(description = "Business transaction id linked to the alert.")
        String transactionId,
        @Schema(description = "Customer identifier linked to the alert.")
        String customerId,
        @Schema(description = "Account identifier linked to the alert.")
        String accountId,
        @Schema(description = "Fraud decision that opened the alert.")
        FraudDecision decision,
        @Schema(description = "Risk score stored with the alert.")
        int riskScore,
        @Schema(description = "Risk level stored with the alert.")
        RiskLevel riskLevel,
        @Schema(description = "Operational alert status.")
        String status,
        @Schema(description = "Timestamp when the alert was created.")
        Instant createdAt,
        @Schema(description = "Timestamp when the alert was last updated.")
        Instant updatedAt,
        @Schema(description = "Timestamp when the alert was closed, when applicable.")
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
