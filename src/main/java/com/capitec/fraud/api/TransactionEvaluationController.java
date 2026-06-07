package com.capitec.fraud.api;

import static java.util.Comparator.comparing;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.capitec.fraud.api.ValidationErrorResponse.FieldValidationError;
import com.capitec.fraud.api.dto.TransactionEvaluationRequest;
import com.capitec.fraud.api.dto.TransactionEvaluationResponse;
import com.capitec.fraud.application.RawPayloadSanitizer;
import com.capitec.fraud.application.TransactionEvaluationCommand;
import com.capitec.fraud.application.TransactionEvaluationService;
import com.capitec.fraud.infrastructure.config.FraudRawPayloadProperties;

import java.time.Clock;
import java.time.Instant;

import jakarta.validation.Validator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({
    ApiPaths.TRANSACTION_EVALUATIONS,
    ApiPaths.TRANSACTIONS_EVALUATE
})
@Tag(name = "Transaction Evaluation", description = "Submit categorized transactions for fraud evaluation.")
public class TransactionEvaluationController
{

    private final TransactionEvaluationService transactionEvaluationService;
    private final RawPayloadSanitizer rawPayloadSanitizer;
    private final FraudRawPayloadProperties rawPayloadProperties;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Clock clock;

    public TransactionEvaluationController(
            TransactionEvaluationService transactionEvaluationService,
            RawPayloadSanitizer rawPayloadSanitizer,
            FraudRawPayloadProperties rawPayloadProperties,
            ObjectMapper objectMapper,
            Validator validator,
            Clock clock)
    {
        this.transactionEvaluationService = transactionEvaluationService;
        this.rawPayloadSanitizer = rawPayloadSanitizer;
        this.rawPayloadProperties = rawPayloadProperties;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.clock = clock;
    }

    @Operation(
            operationId = OpenApiOperationIds.EVALUATE_TRANSACTION,
            summary = "Evaluate a transaction",
            description = "Evaluates a categorized transaction event and returns the deterministic fraud decision.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Transaction was evaluated.",
                content = @Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = TransactionEvaluationResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Request body failed validation.",
                content = @Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Categorized transaction event to evaluate.",
            content = @Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransactionEvaluationRequest.class)))
    @PostMapping
    @PreAuthorize("@fraudSecurityAuthorizer.canEvaluateTransactions(authentication)")
    public TransactionEvaluationResponse evaluate(@RequestBody String rawPayload)
    {
        var request = toRequest(rawPayload);
        var command = new TransactionEvaluationCommand(
                request.toDomain(),
                rawPayloadSanitizer.sanitize(rawPayload),
                Instant.now(clock).plus(rawPayloadProperties.retentionDuration()));
        var evaluation = transactionEvaluationService.evaluate(command);

        return TransactionEvaluationResponse.from(evaluation);
    }

    private TransactionEvaluationRequest toRequest(String rawPayload)
    {
        try
        {
            var request = objectMapper.readValue(rawPayload, TransactionEvaluationRequest.class);
            validate(request);

            return request;
        }
        catch (JsonProcessingException ex)
        {
            throw new IllegalArgumentException("Request body must be valid JSON", ex);
        }
    }

    private void validate(TransactionEvaluationRequest request)
    {
        var fieldErrors = validator.validate(request)
                .stream()
                .sorted(comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> new FieldValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();

        if (!fieldErrors.isEmpty())
        {
            throw new RequestValidationException(fieldErrors);
        }
    }
}
