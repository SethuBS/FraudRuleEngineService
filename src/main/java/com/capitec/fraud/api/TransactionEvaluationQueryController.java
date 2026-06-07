package com.capitec.fraud.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.capitec.fraud.api.dto.TransactionEvaluationResponse;
import com.capitec.fraud.application.FraudRetrievalService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Transaction Evaluation", description = "Retrieve stored transaction evaluation decisions.")
public class TransactionEvaluationQueryController
{

    private final FraudRetrievalService fraudRetrievalService;

    public TransactionEvaluationQueryController(FraudRetrievalService fraudRetrievalService)
    {
        this.fraudRetrievalService = fraudRetrievalService;
    }

    @Operation(
            operationId = OpenApiOperationIds.GET_TRANSACTION_FRAUD_EVALUATION,
            summary = "Get a transaction fraud evaluation",
            description = "Retrieves the stored fraud decision and matched-rule evidence for a transaction id.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Transaction evaluation was found.",
                content = @Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = TransactionEvaluationResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Transaction evaluation was not found.",
                content = @Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping(ApiPaths.TRANSACTION_FRAUD_EVALUATION)
    public TransactionEvaluationResponse fraudEvaluation(
            @Parameter(description = "Transaction id from the original evaluation request.")
            @PathVariable String transactionId)
    {
        var normalizedTransactionId = transactionId.strip();

        return fraudRetrievalService.findEvaluation(normalizedTransactionId)
                .map(TransactionEvaluationResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.transactionEvaluation(normalizedTransactionId));
    }
}
