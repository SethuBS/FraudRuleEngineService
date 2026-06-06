package com.capitec.fraud.application;

import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
class BaselineTransactionEvaluationService implements TransactionEvaluationService
{

    private final RiskPolicy riskPolicy;
    private final Clock clock;

    BaselineTransactionEvaluationService(RiskPolicy riskPolicy, Clock clock)
    {
        this.riskPolicy = riskPolicy;
        this.clock = clock;
    }

    @Override
    public TransactionEvaluation evaluate(Transaction transaction)
    {
        return TransactionEvaluation.from(transaction, List.of(), riskPolicy, Instant.now(clock));
    }
}
