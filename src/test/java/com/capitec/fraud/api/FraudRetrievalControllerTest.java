package com.capitec.fraud.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capitec.fraud.application.FraudAlertSearchQuery;
import com.capitec.fraud.application.FraudAlertView;
import com.capitec.fraud.application.FraudRetrievalService;
import com.capitec.fraud.application.PageResult;
import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.RuleEvaluationResult;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.config.FraudApiConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
    FraudAlertController.class,
    TransactionEvaluationQueryController.class
})
@Import({
    FraudApiConfiguration.class,
    ApiCorrelationIdProvider.class,
    GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
    "fraud.api.paths.fraud-alerts=/test/fraud-alerts",
    "fraud.api.paths.fraud-alert-detail=/test/fraud-alerts/{alertId}",
    "fraud.api.paths.transaction-fraud-evaluation=/test/transactions/{transactionId}/fraud-evaluation",
    "fraud.api.pagination.minimum-page=0",
    "fraud.api.pagination.minimum-size=1",
    "fraud.api.pagination.default-page=0",
    "fraud.api.pagination.default-size=2",
    "fraud.api.pagination.max-size=5"
})
class FraudRetrievalControllerTest
{

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID = "retrieval-correlation-id";
    private static final UUID ALERT_ID = UUID.fromString("b3ed20ca-bac6-45dc-a37e-d61001ee37ab");
    private static final Instant ALERT_CREATED_AT = Instant.parse("2026-06-07T10:00:00Z");
    private static final Instant ALERT_UPDATED_AT = Instant.parse("2026-06-07T10:01:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final Instant FROM_DATE = Instant.parse("2026-06-07T00:00:00Z");
    private static final Instant TO_DATE = Instant.parse("2026-06-08T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Value("${fraud.api.paths.fraud-alerts}")
    private String fraudAlertsPath;

    @Value("${fraud.api.paths.transaction-fraud-evaluation}")
    private String transactionFraudEvaluationPath;

    @MockitoBean
    private FraudRetrievalService fraudRetrievalService;

    @Test
    @WithMockUser
    void fraudAlertListReturnsPaginatedResponseAndFilters()
            throws Exception
    {
        when(fraudRetrievalService.findAlerts(any(FraudAlertSearchQuery.class))).thenReturn(new PageResult<>(
                List.of(sampleAlert()),
                1,
                2,
                7,
                4));

        mockMvc.perform(get(fraudAlertsPath)
                        .param("customerId", " customer-1 ")
                        .param("accountId", "account-1")
                        .param("riskLevel", RiskLevel.HIGH.name())
                        .param("fromDate", FROM_DATE.toString())
                        .param("toDate", TO_DATE.toString())
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(4))
                .andExpect(jsonPath("$.content[0].alertId").value(ALERT_ID.toString()))
                .andExpect(jsonPath("$.content[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$.content[0].decision").value(FraudDecision.FLAGGED.name()))
                .andExpect(jsonPath("$.content[0].riskScore").value(55))
                .andExpect(jsonPath("$.content[0].riskLevel").value(RiskLevel.HIGH.name()))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].createdAt").value(ALERT_CREATED_AT.toString()));

        var queryCaptor = ArgumentCaptor.forClass(FraudAlertSearchQuery.class);
        verify(fraudRetrievalService).findAlerts(queryCaptor.capture());

        var query = queryCaptor.getValue();
        assertThat(query.customerId()).isEqualTo("customer-1");
        assertThat(query.accountId()).isEqualTo("account-1");
        assertThat(query.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(query.fromDate()).isEqualTo(FROM_DATE);
        assertThat(query.toDate()).isEqualTo(TO_DATE);
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(2);
    }

    @Test
    @WithMockUser
    void fraudAlertListUsesConfiguredPaginationDefaults()
            throws Exception
    {
        when(fraudRetrievalService.findAlerts(any(FraudAlertSearchQuery.class))).thenReturn(new PageResult<>(
                List.of(),
                0,
                2,
                0,
                0));

        mockMvc.perform(get(fraudAlertsPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));

        var queryCaptor = ArgumentCaptor.forClass(FraudAlertSearchQuery.class);
        verify(fraudRetrievalService).findAlerts(queryCaptor.capture());

        assertThat(queryCaptor.getValue().page()).isZero();
        assertThat(queryCaptor.getValue().size()).isEqualTo(2);
    }

    @Test
    @WithMockUser
    void fraudAlertListRejectsInvalidPagination()
            throws Exception
    {
        mockMvc.perform(get(fraudAlertsPath)
                        .param("page", "-1")
                        .param("size", "6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("page", "size")));

        verifyNoInteractions(fraudRetrievalService);
    }

    @Test
    @WithMockUser
    void fraudAlertListRejectsInvalidDateRange()
            throws Exception
    {
        mockMvc.perform(get(fraudAlertsPath)
                        .param("fromDate", TO_DATE.toString())
                        .param("toDate", FROM_DATE.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fromDate"));

        verifyNoInteractions(fraudRetrievalService);
    }

    @Test
    @WithMockUser
    void fraudAlertDetailReturnsAlert()
            throws Exception
    {
        when(fraudRetrievalService.findAlert(ALERT_ID)).thenReturn(Optional.of(sampleAlert()));

        mockMvc.perform(get(fraudAlertsPath + "/" + ALERT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value(ALERT_ID.toString()))
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.accountId").value("account-1"))
                .andExpect(jsonPath("$.riskLevel").value(RiskLevel.HIGH.name()));
    }

    @Test
    @WithMockUser
    void missingFraudAlertReturnsNotFound()
            throws Exception
    {
        when(fraudRetrievalService.findAlert(ALERT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(fraudAlertsPath + "/" + ALERT_ID)
                        .header(CORRELATION_HEADER, CORRELATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(header().string(CORRELATION_HEADER, CORRELATION_ID));
    }

    @Test
    @WithMockUser
    void transactionEvaluationLookupReturnsStoredEvaluation()
            throws Exception
    {
        when(fraudRetrievalService.findEvaluation("tx-1")).thenReturn(Optional.of(sampleEvaluation()));

        mockMvc.perform(get(transactionFraudEvaluationPath.replace("{transactionId}", "tx-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.decision").value(FraudDecision.FLAGGED.name()))
                .andExpect(jsonPath("$.riskScore").value(55))
                .andExpect(jsonPath("$.riskLevel").value(RiskLevel.HIGH.name()))
                .andExpect(jsonPath("$.matchedRules[0].ruleCode").value("HIGH_AMOUNT"))
                .andExpect(jsonPath("$.evaluatedAt").value(EVALUATED_AT.toString()));
    }

    @Test
    @WithMockUser
    void missingTransactionEvaluationReturnsNotFound()
            throws Exception
    {
        when(fraudRetrievalService.findEvaluation("missing-tx")).thenReturn(Optional.empty());

        mockMvc.perform(get(transactionFraudEvaluationPath.replace("{transactionId}", "missing-tx")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private static FraudAlertView sampleAlert()
    {
        return new FraudAlertView(
                ALERT_ID,
                "tx-1",
                "customer-1",
                "account-1",
                FraudDecision.FLAGGED,
                55,
                RiskLevel.HIGH,
                "OPEN",
                ALERT_CREATED_AT,
                ALERT_UPDATED_AT,
                null);
    }

    private static TransactionEvaluation sampleEvaluation()
    {
        return TransactionEvaluation.from(
                sampleTransaction(),
                List.of(RuleEvaluationResult.matched(
                        "HIGH_AMOUNT",
                        "High Amount",
                        55,
                        "Amount exceeded configured threshold",
                        EVALUATED_AT)),
                baselinePolicy(),
                EVALUATED_AT);
    }

    private static Transaction sampleTransaction()
    {
        return new Transaction(
                "event-1",
                "tx-1",
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                Instant.parse("2026-06-07T08:00:00Z"),
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }

    private static RiskPolicy baselinePolicy()
    {
        return new RiskPolicy(
                RiskScore.of(25),
                RiskScore.of(50),
                RiskScore.of(75),
                RiskScore.of(100),
                RiskLevel.MEDIUM,
                RiskLevel.HIGH);
    }
}
