package com.capitec.fraud.application;

import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

public interface TransactionEvaluationService
{

    TransactionEvaluation evaluate(Transaction transaction);
}
