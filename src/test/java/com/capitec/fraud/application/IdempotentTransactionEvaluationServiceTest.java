package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.persistence.TransactionEvaluationPersistenceService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class IdempotentTransactionEvaluationServiceTest
{

    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final RiskPolicy RISK_POLICY = new RiskPolicy(
            RiskScore.of(25),
            RiskScore.of(50),
            RiskScore.of(75),
            RiskLevel.MEDIUM,
            RiskLevel.HIGH);

    @Test
    void duplicateRaceReturnsPreviousEvaluationWhenConstraintWins()
    {
        var engine = mock(TransactionEvaluationEngine.class);
        var persistenceService = mock(TransactionEvaluationPersistenceService.class);
        var service = new IdempotentTransactionEvaluationService(engine, persistenceService, RISK_POLICY);
        var transaction = sampleTransaction("event-1", "tx-1");
        var evaluation = TransactionEvaluation.from(transaction, List.of(), RISK_POLICY, EVALUATED_AT);

        when(persistenceService.findEvaluationByEventId(transaction.eventId(), RISK_POLICY))
                .thenReturn(Optional.empty(), Optional.of(evaluation));
        when(persistenceService.findEvaluationByTransactionId(transaction.transactionId(), RISK_POLICY))
                .thenReturn(Optional.empty());
        when(engine.evaluate(transaction)).thenReturn(evaluation);
        when(persistenceService.persistProcessedEvaluation(evaluation))
                .thenThrow(new DataIntegrityViolationException("duplicate event"));

        var result = service.evaluate(transaction);

        assertThat(result).isSameAs(evaluation);
        verify(engine).evaluate(transaction);
        verify(persistenceService).persistProcessedEvaluation(evaluation);
        verify(persistenceService, times(2)).findEvaluationByEventId(transaction.eventId(), RISK_POLICY);
        verify(persistenceService).findEvaluationByTransactionId(transaction.transactionId(), RISK_POLICY);
    }

    private static Transaction sampleTransaction(String eventId, String transactionId)
    {
        return new Transaction(
                eventId,
                transactionId,
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                TRANSACTION_TIME,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }
}
