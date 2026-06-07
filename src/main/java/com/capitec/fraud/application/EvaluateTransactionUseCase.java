package com.capitec.fraud.application;

import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class EvaluateTransactionUseCase implements TransactionEvaluationService
{

    private final TransactionalEvaluationOperation transactionalEvaluationOperation;

    public EvaluateTransactionUseCase(TransactionalEvaluationOperation transactionalEvaluationOperation)
    {
        this.transactionalEvaluationOperation = transactionalEvaluationOperation;
    }

    @Override
    public TransactionEvaluation evaluate(Transaction transaction)
    {
        return evaluate(TransactionEvaluationCommand.withoutRawPayload(transaction));
    }

    @Override
    public TransactionEvaluation evaluate(TransactionEvaluationCommand command)
    {
        try
        {
            return transactionalEvaluationOperation.evaluate(command);
        }
        catch (DataIntegrityViolationException ex)
        {
            return transactionalEvaluationOperation.findExistingEvaluation(command.transaction())
                    .orElseThrow(() -> new TransactionEvaluationException(
                            "Transaction evaluation could not be completed safely",
                            ex));
        }
    }
}
