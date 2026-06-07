package com.capitec.fraud.application;

import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.persistence.TransactionEvaluationPersistenceService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
class IdempotentTransactionEvaluationService implements TransactionEvaluationService
{

    private final TransactionEvaluationEngine transactionEvaluationEngine;
    private final TransactionEvaluationPersistenceService persistenceService;
    private final RiskPolicy riskPolicy;

    IdempotentTransactionEvaluationService(
            TransactionEvaluationEngine transactionEvaluationEngine,
            TransactionEvaluationPersistenceService persistenceService,
            RiskPolicy riskPolicy)
    {
        this.transactionEvaluationEngine = transactionEvaluationEngine;
        this.persistenceService = persistenceService;
        this.riskPolicy = riskPolicy;
    }

    @Override
    public TransactionEvaluation evaluate(Transaction transaction)
    {
        return findExistingEvaluation(transaction)
                .orElseGet(() -> evaluateAndPersist(transaction));
    }

    private java.util.Optional<TransactionEvaluation> findExistingEvaluation(Transaction transaction)
    {
        return persistenceService.findEvaluationByEventId(transaction.eventId(), riskPolicy)
                .or(() -> persistenceService.findEvaluationByTransactionId(transaction.transactionId(), riskPolicy));
    }

    private TransactionEvaluation evaluateAndPersist(Transaction transaction)
    {
        var evaluation = transactionEvaluationEngine.evaluate(transaction);

        try
        {
            return persistenceService.persistProcessedEvaluation(evaluation);
        }
        catch (DataIntegrityViolationException ex)
        {
            return findExistingEvaluation(transaction).orElseThrow(() -> ex);
        }
    }
}
