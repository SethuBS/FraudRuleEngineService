package com.capitec.fraud.api;

import com.capitec.fraud.api.dto.TransactionEvaluationResponse;
import com.capitec.fraud.application.FraudRetrievalService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionEvaluationQueryController
{

    private final FraudRetrievalService fraudRetrievalService;

    public TransactionEvaluationQueryController(FraudRetrievalService fraudRetrievalService)
    {
        this.fraudRetrievalService = fraudRetrievalService;
    }

    @GetMapping(ApiPaths.TRANSACTION_FRAUD_EVALUATION)
    public TransactionEvaluationResponse fraudEvaluation(@PathVariable String transactionId)
    {
        var normalizedTransactionId = transactionId.strip();

        return fraudRetrievalService.findEvaluation(normalizedTransactionId)
                .map(TransactionEvaluationResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.transactionEvaluation(normalizedTransactionId));
    }
}
