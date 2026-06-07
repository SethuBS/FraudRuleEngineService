package com.capitec.fraud.application;

import com.capitec.fraud.domain.RiskLevel;

import java.time.Instant;

public record FraudAlertSearchQuery(
        String customerId,
        String accountId,
        RiskLevel riskLevel,
        Instant fromDate,
        Instant toDate,
        int page,
        int size)
{
}
