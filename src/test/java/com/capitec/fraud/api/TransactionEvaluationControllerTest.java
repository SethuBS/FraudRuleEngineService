package com.capitec.fraud.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capitec.fraud.application.RawPayloadSanitizer;
import com.capitec.fraud.application.TransactionEvaluationCommand;
import com.capitec.fraud.application.TransactionEvaluationService;
import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.RuleEvaluationResult;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.config.FraudRawPayloadConfiguration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionEvaluationController.class)
@Import({
    FraudRawPayloadConfiguration.class,
    RawPayloadSanitizer.class,
    TransactionEvaluationControllerTest.FixedClockConfiguration.class
})
@TestPropertySource(properties = {
    "fraud.api.paths.transaction-evaluations=/test/transaction-evaluations",
    "fraud.api.validation.merchant-category-max-length=8",
    "fraud.raw-payload.retention-duration=PT2H",
    "fraud.raw-payload.redacted-value=MASKED",
    "fraud.raw-payload.sensitive-field-names=cardNumber,authorization,email,token,accountNumber"
})
class TransactionEvaluationControllerTest
{

    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T09:00:00Z");
    private static final Duration RETENTION_DURATION = Duration.ofHours(2);

    @Autowired
    private MockMvc mockMvc;

    @Value("${fraud.api.paths.transaction-evaluations}")
    private String transactionEvaluationsPath;

    @MockitoBean
    private TransactionEvaluationService transactionEvaluationService;

    @Test
    @WithMockUser
    void invalidRequestReturnsBadRequest()
            throws Exception
    {
        mockMvc.perform(post(transactionEvaluationsPath)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder(
                        "accountId",
                        "amount",
                        "currency",
                        "customerId",
                        "eventId",
                        "merchantCategory",
                        "transactionId",
                        "transactionTimestamp")));

        verifyNoInteractions(transactionEvaluationService);
    }

    @Test
    @WithMockUser
    void invalidAmountAndCurrencyReturnBadRequest()
            throws Exception
    {
        mockMvc.perform(post(transactionEvaluationsPath)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "event-1",
                                  "transactionId": "tx-1",
                                  "customerId": "customer-1",
                                  "accountId": "account-1",
                                  "amount": 0,
                                  "currency": "not-a-currency",
                                  "transactionTimestamp": "2026-06-07T08:00:00Z",
                                  "merchantCategory": "grocery"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("amount", "currency")));

        verifyNoInteractions(transactionEvaluationService);
    }

    @Test
    @WithMockUser
    void invalidMerchantCategoryLengthReturnsBadRequest()
            throws Exception
    {
        mockMvc.perform(post(transactionEvaluationsPath)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "event-1",
                                  "transactionId": "tx-1",
                                  "customerId": "customer-1",
                                  "accountId": "account-1",
                                  "amount": 100.50,
                                  "currency": "ZAR",
                                  "transactionTimestamp": "2026-06-07T08:00:00Z",
                                  "merchantCategory": "grocery-store"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("merchantCategory")));

        verifyNoInteractions(transactionEvaluationService);
    }

    @Test
    @WithMockUser
    void validRequestMapsToDomainModelAndReturnsStableResponse()
            throws Exception
    {
        when(transactionEvaluationService.evaluate(any(TransactionEvaluationCommand.class))).thenAnswer(invocation ->
        {
            TransactionEvaluationCommand command = invocation.getArgument(0);
            Transaction transaction = command.transaction();

            return TransactionEvaluation.from(
                    transaction,
                    List.of(RuleEvaluationResult.matched(
                            "HIGH_AMOUNT",
                            "High Amount",
                            55,
                            "Amount exceeded configured threshold",
                            EVALUATED_AT)),
                    baselinePolicy(),
                    EVALUATED_AT);
        });

        mockMvc.perform(post(transactionEvaluationsPath)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": " event-1 ",
                                  "transactionId": " tx-1 ",
                                  "customerId": " customer-1 ",
                                  "accountId": " account-1 ",
                                  "amount": 100.50,
                                  "currency": "zar",
                                  "transactionTimestamp": "2026-06-07T08:00:00Z",
                                  "merchantCategory": "grocery",
                                  "country": "za",
                                  "channel": "mobile",
                                  "merchantId": "merchant-1",
                                  "merchantName": "Corner Shop",
                                  "deviceId": "device-1",
                                  "cardNumber": "4111111111111111",
                                  "authorization": "Bearer secret-token",
                                  "email": "customer@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("event-1"))
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.accountId").value("account-1"))
                .andExpect(jsonPath("$.decision").value(FraudDecision.FLAGGED.name()))
                .andExpect(jsonPath("$.riskScore").value(55))
                .andExpect(jsonPath("$.riskLevel").value(RiskLevel.HIGH.name()))
                .andExpect(jsonPath("$.matchedRules[0].ruleCode").value("HIGH_AMOUNT"))
                .andExpect(jsonPath("$.matchedRules[0].ruleName").value("High Amount"))
                .andExpect(jsonPath("$.matchedRules[0].scoreContribution").value(55))
                .andExpect(jsonPath("$.matchedRules[0].explanation").value("Amount exceeded configured threshold"))
                .andExpect(jsonPath("$.evaluatedAt").value("2026-06-07T09:00:00Z"));

        var commandCaptor = ArgumentCaptor.forClass(TransactionEvaluationCommand.class);
        verify(transactionEvaluationService).evaluate(commandCaptor.capture());

        var command = commandCaptor.getValue();
        var transaction = command.transaction();
        assertThat(transaction.eventId()).isEqualTo("event-1");
        assertThat(transaction.transactionId()).isEqualTo("tx-1");
        assertThat(transaction.customerId()).isEqualTo("customer-1");
        assertThat(transaction.accountId()).isEqualTo("account-1");
        assertThat(transaction.amount().amount()).isEqualByComparingTo("100.50");
        assertThat(transaction.amount().currencyCode()).isEqualTo("ZAR");
        assertThat(transaction.category().code()).isEqualTo("GROCERY");
        assertThat(transaction.transactionTime()).isEqualTo(Instant.parse("2026-06-07T08:00:00Z"));
        assertThat(transaction.country()).isEqualTo("ZA");
        assertThat(transaction.channel()).isEqualTo("MOBILE");
        assertThat(transaction.merchantId()).isEqualTo("merchant-1");
        assertThat(transaction.merchantName()).isEqualTo("Corner Shop");
        assertThat(transaction.deviceId()).isEqualTo("device-1");
        assertThat(command.rawPayloadExpiresAt()).isEqualTo(EVALUATED_AT.plus(RETENTION_DURATION));
        assertThat(command.sanitizedRawPayload())
                .contains("\"cardNumber\":\"MASKED\"")
                .contains("\"authorization\":\"MASKED\"")
                .contains("\"email\":\"MASKED\"")
                .contains("\"customerId\":\" customer-1 \"")
                .doesNotContain("4111111111111111")
                .doesNotContain("secret-token")
                .doesNotContain("customer@example.com");
    }

    private static RiskPolicy baselinePolicy()
    {
        return new RiskPolicy(
                RiskScore.of(25),
                RiskScore.of(50),
                RiskScore.of(75),
                RiskLevel.MEDIUM,
                RiskLevel.HIGH);
    }

    @TestConfiguration
    static class FixedClockConfiguration
    {

        @Bean
        Clock fixedClock()
        {
            return Clock.fixed(EVALUATED_AT, ZoneOffset.UTC);
        }
    }
}
