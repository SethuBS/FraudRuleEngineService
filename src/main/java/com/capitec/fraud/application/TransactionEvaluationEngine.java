package com.capitec.fraud.application;

import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

interface TransactionEvaluationEngine
{

    TransactionEvaluation evaluate(Transaction transaction);
}
