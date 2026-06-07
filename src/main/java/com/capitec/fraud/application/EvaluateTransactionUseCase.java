package com.capitec.fraud.application;

import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class EvaluateTransactionUseCase implements TransactionEvaluationService
{

    private static final Logger LOG = LoggerFactory.getLogger(EvaluateTransactionUseCase.class);

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
        var transaction = command.transaction();
        logEvaluationStarted(transaction);

        try
        {
            var evaluation = transactionalEvaluationOperation.evaluate(command);
            logEvaluationCompleted(evaluation);

            return evaluation;
        }
        catch (DataIntegrityViolationException ex)
        {
            return transactionalEvaluationOperation.findExistingEvaluation(transaction)
                    .map(evaluation ->
                    {
                        logEvaluationCompleted(evaluation);

                        return evaluation;
                    })
                    .orElseThrow(() ->
                    {
                        logEvaluationDuplicateRaceUnresolved(transaction);

                        return new TransactionEvaluationException(
                                "Transaction evaluation could not be completed safely",
                                ex);
                    });
        }
    }

    private static void logEvaluationStarted(Transaction transaction)
    {
        LOG.info(
                "event=transaction_evaluation_started eventId={} transactionId={}",
                transaction.eventId(),
                transaction.transactionId());
    }

    private static void logEvaluationCompleted(TransactionEvaluation evaluation)
    {
        LOG.info(
                "event=transaction_evaluation_completed eventId={} transactionId={} decision={} riskScore={} "
                        + "riskLevel={} matchedRuleCount={} evaluatedRuleCount={}",
                evaluation.transaction().eventId(),
                evaluation.transaction().transactionId(),
                evaluation.decision(),
                evaluation.riskScore().value(),
                evaluation.riskLevel(),
                evaluation.matchedRules().size(),
                evaluation.ruleResults().size());
    }

    private static void logEvaluationDuplicateRaceUnresolved(Transaction transaction)
    {
        LOG.warn(
                "event=transaction_evaluation_duplicate_race_unresolved eventId={} transactionId={}",
                transaction.eventId(),
                transaction.transactionId());
    }
}
