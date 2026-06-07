package com.capitec.fraud.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.capitec.fraud.api.ValidationErrorResponse.FieldValidationError;
import com.capitec.fraud.api.dto.FraudAlertResponse;
import com.capitec.fraud.api.dto.PagedResponse;
import com.capitec.fraud.application.FraudAlertSearchQuery;
import com.capitec.fraud.application.FraudRetrievalService;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.infrastructure.config.FraudApiPaginationProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Fraud Alerts", description = "Retrieve stored fraud alerts and alert metadata.")
public class FraudAlertController
{

    private final FraudRetrievalService fraudRetrievalService;
    private final FraudApiPaginationProperties paginationProperties;

    public FraudAlertController(
            FraudRetrievalService fraudRetrievalService,
            FraudApiPaginationProperties paginationProperties)
    {
        this.fraudRetrievalService = fraudRetrievalService;
        this.paginationProperties = paginationProperties;
    }

    @Operation(
            operationId = OpenApiOperationIds.LIST_FRAUD_ALERTS,
            summary = "List fraud alerts",
            description = "Retrieves stored fraud alerts using optional filters and configured pagination defaults.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Fraud alerts were retrieved.",
                content = @Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = PagedResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Filter or pagination value failed validation.",
                content = @Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping(ApiPaths.FRAUD_ALERTS)
    public PagedResponse<FraudAlertResponse> fraudAlerts(
            @Parameter(description = "Filter alerts by customer id.")
            @RequestParam(required = false) String customerId,
            @Parameter(description = "Filter alerts by account id.")
            @RequestParam(required = false) String accountId,
            @Parameter(description = "Filter alerts by risk level.")
            @RequestParam(required = false) RiskLevel riskLevel,
            @Parameter(description = "Filter alerts created at or after this timestamp.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter alerts created at or before this timestamp.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @Parameter(description = "Zero-based result page.")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Result page size.")
            @RequestParam(required = false) Integer size)
    {
        var resolvedPage = paginationProperties.resolvePage(page);
        var resolvedSize = paginationProperties.resolveSize(size);
        validatePagination(resolvedPage, resolvedSize);
        validateDateRange(fromDate, toDate);

        var alerts = fraudRetrievalService.findAlerts(new FraudAlertSearchQuery(
                optionalText(customerId),
                optionalText(accountId),
                riskLevel,
                fromDate,
                toDate,
                resolvedPage,
                resolvedSize));

        return PagedResponse.from(alerts, FraudAlertResponse::from);
    }

    @Operation(
            operationId = OpenApiOperationIds.GET_FRAUD_ALERT,
            summary = "Get a fraud alert",
            description = "Retrieves one stored fraud alert by alert id.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Fraud alert was found.",
                content = @Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = FraudAlertResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Fraud alert was not found.",
                content = @Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping(ApiPaths.FRAUD_ALERT_DETAIL)
    public FraudAlertResponse fraudAlert(
            @Parameter(description = "Fraud alert id.")
            @PathVariable UUID alertId)
    {
        return fraudRetrievalService.findAlert(alertId)
                .map(FraudAlertResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.fraudAlert(alertId));
    }

    private void validatePagination(int page, int size)
    {
        var fieldErrors = new java.util.ArrayList<FieldValidationError>();
        if (!paginationProperties.validPage(page))
        {
            fieldErrors.add(new FieldValidationError(
                    "page",
                    "must be greater than or equal to " + paginationProperties.minimumPage()));
        }
        if (!paginationProperties.validSize(size))
        {
            fieldErrors.add(new FieldValidationError(
                    "size",
                    "must be between "
                            + paginationProperties.minimumSize()
                            + " and "
                            + paginationProperties.maxSize()));
        }
        if (!fieldErrors.isEmpty())
        {
            throw new RequestValidationException(fieldErrors);
        }
    }

    private static void validateDateRange(Instant fromDate, Instant toDate)
    {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
        {
            throw new RequestValidationException(List.of(new FieldValidationError(
                    "fromDate",
                    "must be before or equal to toDate")));
        }
    }

    private static String optionalText(String value)
    {
        if (value == null || value.isBlank())
        {
            return null;
        }

        return value.strip();
    }
}
