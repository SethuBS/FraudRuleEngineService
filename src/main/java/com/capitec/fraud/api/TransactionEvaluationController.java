package com.capitec.fraud.api;

import com.capitec.fraud.api.dto.TransactionEvaluationRequest;
import com.capitec.fraud.api.dto.TransactionEvaluationResponse;
import com.capitec.fraud.application.TransactionEvaluationService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.TRANSACTION_EVALUATIONS)
public class TransactionEvaluationController
{

    private final TransactionEvaluationService transactionEvaluationService;

    public TransactionEvaluationController(TransactionEvaluationService transactionEvaluationService)
    {
        this.transactionEvaluationService = transactionEvaluationService;
    }

    @PostMapping
    public TransactionEvaluationResponse evaluate(@Valid @RequestBody TransactionEvaluationRequest request)
    {
        var evaluation = transactionEvaluationService.evaluate(request.toDomain());

        return TransactionEvaluationResponse.from(evaluation);
    }
}
