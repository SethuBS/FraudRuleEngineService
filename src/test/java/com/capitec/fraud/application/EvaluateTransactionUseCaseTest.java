package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.domain.TransactionEvaluation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class EvaluateTransactionUseCaseTest
{

    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final RiskPolicy RISK_POLICY = new RiskPolicy(
            RiskScore.of(25),
            RiskScore.of(50),
            RiskScore.of(75),
            RiskScore.of(100),
            RiskLevel.MEDIUM,
            RiskLevel.HIGH);

    @Test
    void duplicateRaceReturnsPreviousEvaluationWhenConstraintWins()
    {
        var operation = mock(TransactionalEvaluationOperation.class);
        var useCase = new EvaluateTransactionUseCase(operation);
        var transaction = sampleTransaction("event-1", "tx-1");
        var command = TransactionEvaluationCommand.withoutRawPayload(transaction);
        var evaluation = TransactionEvaluation.from(transaction, List.of(), RISK_POLICY, EVALUATED_AT);

        when(operation.evaluate(command)).thenThrow(new DataIntegrityViolationException("duplicate event"));
        when(operation.findExistingEvaluation(transaction)).thenReturn(Optional.of(evaluation));

        var result = useCase.evaluate(command);

        assertThat(result).isSameAs(evaluation);
        verify(operation).evaluate(command);
        verify(operation).findExistingEvaluation(transaction);
    }

    @Test
    void duplicateRaceWithoutStoredEvaluationThrowsSafeApplicationException()
    {
        var operation = mock(TransactionalEvaluationOperation.class);
        var useCase = new EvaluateTransactionUseCase(operation);
        var transaction = sampleTransaction("event-1", "tx-1");
        var command = TransactionEvaluationCommand.withoutRawPayload(transaction);

        when(operation.evaluate(command)).thenThrow(new DataIntegrityViolationException("duplicate event"));
        when(operation.findExistingEvaluation(transaction)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.evaluate(command))
                .isInstanceOf(TransactionEvaluationException.class)
                .hasMessage("Transaction evaluation could not be completed safely");
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
