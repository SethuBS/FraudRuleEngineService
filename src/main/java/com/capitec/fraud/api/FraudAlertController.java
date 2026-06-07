package com.capitec.fraud.api;

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

@RestController
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

    @GetMapping(ApiPaths.FRAUD_ALERTS)
    public PagedResponse<FraudAlertResponse> fraudAlerts(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(required = false) Integer page,
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

    @GetMapping(ApiPaths.FRAUD_ALERT_DETAIL)
    public FraudAlertResponse fraudAlert(@PathVariable UUID alertId)
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
