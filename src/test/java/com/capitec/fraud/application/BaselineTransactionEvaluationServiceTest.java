package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class BaselineTransactionEvaluationServiceTest
{

    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");

    @Test
    void evaluatesTransactionWithConfiguredPolicyAndClock()
    {
        var service = new BaselineTransactionEvaluationService(
                baselinePolicy(),
                Clock.fixed(EVALUATED_AT, ZoneOffset.UTC));

        var evaluation = service.evaluate(sampleTransaction());

        assertThat(evaluation.decision()).isEqualTo(FraudDecision.APPROVED);
        assertThat(evaluation.riskScore()).isEqualTo(RiskScore.ZERO);
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(evaluation.matchedRules()).isEmpty();
        assertThat(evaluation.evaluatedAt()).isEqualTo(EVALUATED_AT);
    }

    private static RiskPolicy baselinePolicy()
    {
        return new RiskPolicy(
                RiskScore.of(25),
                RiskScore.of(50),
                RiskScore.of(75),
                RiskLevel.MEDIUM,
                RiskLevel.HIGH);
    }

    private static Transaction sampleTransaction()
    {
        return new Transaction(
                "event-1",
                "tx-1",
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                Instant.parse("2026-06-07T08:00:00Z"),
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }
}
