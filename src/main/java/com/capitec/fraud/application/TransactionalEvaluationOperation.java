package com.capitec.fraud.application;

import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.persistence.TransactionEvaluationPersistenceService;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TransactionalEvaluationOperation
{

    private final TransactionEvaluationEngine transactionEvaluationEngine;
    private final TransactionEvaluationPersistenceService persistenceService;
    private final RiskPolicy riskPolicy;

    TransactionalEvaluationOperation(
            TransactionEvaluationEngine transactionEvaluationEngine,
            TransactionEvaluationPersistenceService persistenceService,
            RiskPolicy riskPolicy)
    {
        this.transactionEvaluationEngine = transactionEvaluationEngine;
        this.persistenceService = persistenceService;
        this.riskPolicy = riskPolicy;
    }

    @Transactional
    public TransactionEvaluation evaluate(TransactionEvaluationCommand command)
    {
        var transaction = command.transaction();

        return findExistingEvaluation(transaction)
                .orElseGet(() -> evaluateAndPersist(command));
    }

    @Transactional(readOnly = true)
    public Optional<TransactionEvaluation> findExistingEvaluation(Transaction transaction)
    {
        return persistenceService.findEvaluationByEventId(transaction.eventId(), riskPolicy)
                .or(() -> persistenceService.findEvaluationByTransactionId(transaction.transactionId(), riskPolicy));
    }

    private TransactionEvaluation evaluateAndPersist(TransactionEvaluationCommand command)
    {
        var evaluation = transactionEvaluationEngine.evaluate(command.transaction());

        return persistenceService.persistProcessedEvaluation(
                evaluation,
                command.sanitizedRawPayload(),
                command.rawPayloadExpiresAt());
    }
}
