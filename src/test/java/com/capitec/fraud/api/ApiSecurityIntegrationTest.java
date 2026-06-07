package com.capitec.fraud.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.RuleEvaluationRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.rules.HighValueTransactionRule;
import com.capitec.fraud.rules.RiskyMerchantCategoryRule;
import com.capitec.fraud.rules.SuspiciousMerchantRule;
import com.capitec.fraud.support.PostgresIntegrationTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ApiSecurityIntegrationTest.FixedClockConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "fraud.evaluation.maximum-score=100",
    "fraud.evaluation.risk-thresholds.medium=25",
    "fraud.evaluation.risk-thresholds.high=50",
    "fraud.evaluation.risk-thresholds.critical=75",
    "fraud.evaluation.decision-thresholds.review=MEDIUM",
    "fraud.evaluation.decision-thresholds.flagged=HIGH",
    "fraud.rule-catalog.enabled-by-default=true",
    "fraud.rule-catalog.seed-on-startup=true",
    "fraud.raw-payload.cleanup.enabled=false",
    "fraud.rules.high-value-transaction.threshold-amount=100.00",
    "fraud.rules.high-value-transaction.default-score=55",
    "fraud.rules.high-value-transaction.severity=HIGH",
    "fraud.rules.risky-merchant-category.risk-categories=GAMBLING,CRYPTO,JEWELRY",
    "fraud.rules.risky-merchant-category.default-score=30",
    "fraud.rules.risky-merchant-category.severity=MEDIUM",
    "fraud.rules.suspicious-merchant.merchant-ids=MERCHANT-WATCHLIST",
    "fraud.rules.suspicious-merchant.merchant-name-fragments=WATCHLISTED,HIGH RISK TRADERS",
    "fraud.rules.suspicious-merchant.default-score=50",
    "fraud.rules.suspicious-merchant.severity=HIGH"
})
class ApiSecurityIntegrationTest extends PostgresIntegrationTest
{

    private static final Instant EVALUATED_AT = Instant.parse("2026-06-16T09:00:00Z");
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-16T08:55:00Z");
    private static final BigDecimal HIGH_RISK_AMOUNT = new BigDecimal("250.00");
    private static final int FIRST_ALERT_INDEX = 0;
    private static final long SINGLE_ALERT_COUNT = 1L;
    private static final String EVENT_ID = "event-api-security-1";
    private static final String TRANSACTION_ID = "tx-api-security-1";
    private static final String CUSTOMER_ID = "customer-api-security-1";
    private static final String ACCOUNT_ID = "account-api-security-1";
    private static final String CURRENCY = "ZAR";
    private static final String MERCHANT_CATEGORY = "GAMBLING";
    private static final String COUNTRY = "ZA";
    private static final String CHANNEL = "MOBILE";
    private static final String MERCHANT_ID = "MERCHANT-WATCHLIST";
    private static final String MERCHANT_NAME = "Watchlisted Traders";
    private static final String DEVICE_ID = "device-api-security-1";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MISSING_TRANSACTION_ID = "missing-transaction";
    private static final String TRANSACTION_ID_PLACEHOLDER = "{transactionId}";
    private static final String EMPTY_JSON = "{}";
    private static final String VALIDATION_FAILED_CODE = "VALIDATION_FAILED";
    private static final String AUTHENTICATION_REQUIRED_CODE = "AUTHENTICATION_REQUIRED";
    private static final String ACCESS_DENIED_CODE = "ACCESS_DENIED";
    private static final String RESOURCE_NOT_FOUND_CODE = "RESOURCE_NOT_FOUND";

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = createPostgresContainer();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @jakarta.annotation.Resource
    private FraudAlertRepository fraudAlertRepository;

    @jakarta.annotation.Resource
    private RuleEvaluationRepository ruleEvaluationRepository;

    @jakarta.annotation.Resource
    private TransactionRepository transactionRepository;

    @jakarta.annotation.Resource
    private ProcessedEventRepository processedEventRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Value("${fraud.api.paths.transactions-evaluate}")
    private String transactionsEvaluatePath;

    @Value("${fraud.api.paths.fraud-alerts}")
    private String fraudAlertsPath;

    @Value("${fraud.api.paths.transaction-fraud-evaluation}")
    private String transactionFraudEvaluationPath;

    @Value("${fraud.security.scope-authority-prefix}")
    private String scopeAuthorityPrefix;

    @Value("${fraud.security.transaction-evaluate-scope}")
    private String transactionEvaluateScope;

    @Value("${fraud.security.fraud-alerts-read-scope}")
    private String fraudAlertsReadScope;

    @Value("${fraud.evaluation.maximum-score}")
    private int maximumRiskScore;

    @Value("${fraud.api.pagination.default-page}")
    private int defaultPage;

    @Value("${fraud.api.pagination.default-size}")
    private int defaultPageSize;

    @Value("${fraud.persistence.default-alert-status}")
    private String defaultAlertStatus;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry)
    {
        registerDatasourceProperties(registry, POSTGRESQL);
    }

    @BeforeEach
    void resetDatabase()
    {
        fraudAlertRepository.deleteAll();
        ruleEvaluationRepository.deleteAll();
        transactionRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void postTransactionEvaluateHappyPathPersistsDecision()
            throws Exception
    {
        mockMvc.perform(post(transactionsEvaluatePath)
                        .with(scope(transactionEvaluateScope))
                        .contentType(APPLICATION_JSON)
                        .content(highRiskTransactionRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.decision").value(FraudDecision.FLAGGED.name()))
                .andExpect(jsonPath("$.riskScore").value(maximumRiskScore))
                .andExpect(jsonPath("$.riskLevel").value(RiskLevel.CRITICAL.name()))
                .andExpect(jsonPath("$.matchedRules[*].ruleCode", containsInAnyOrder(
                        HighValueTransactionRule.RULE_CODE,
                        RiskyMerchantCategoryRule.RULE_CODE,
                        SuspiciousMerchantRule.RULE_CODE)))
                .andExpect(jsonPath("$.evaluatedAt").value(EVALUATED_AT.toString()));
    }

    @Test
    void getFraudAlertsHappyPathReturnsPersistedAlert()
            throws Exception
    {
        submitHighRiskTransaction();

        mockMvc.perform(get(fraudAlertsPath)
                        .with(scope(fraudAlertsReadScope))
                        .param("customerId", CUSTOMER_ID)
                        .param("riskLevel", RiskLevel.CRITICAL.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(defaultPage))
                .andExpect(jsonPath("$.size").value(defaultPageSize))
                .andExpect(jsonPath("$.totalElements").value(SINGLE_ALERT_COUNT))
                .andExpect(jsonPath("$.content[" + FIRST_ALERT_INDEX + "].transactionId").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.content[" + FIRST_ALERT_INDEX + "].customerId").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.content[" + FIRST_ALERT_INDEX + "].accountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.content[" + FIRST_ALERT_INDEX + "].decision").value(FraudDecision.FLAGGED.name()))
                .andExpect(jsonPath("$.content[" + FIRST_ALERT_INDEX + "].riskScore").value(maximumRiskScore))
                .andExpect(jsonPath("$.content[" + FIRST_ALERT_INDEX + "].riskLevel").value(RiskLevel.CRITICAL.name()))
                .andExpect(jsonPath("$.content[" + FIRST_ALERT_INDEX + "].status").value(defaultAlertStatus));
    }

    @Test
    void getTransactionFraudEvaluationHappyPathReturnsPersistedEvaluation()
            throws Exception
    {
        submitHighRiskTransaction();

        mockMvc.perform(get(transactionFraudEvaluationPath(TRANSACTION_ID))
                        .with(scope(fraudAlertsReadScope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.decision").value(FraudDecision.FLAGGED.name()))
                .andExpect(jsonPath("$.riskScore").value(maximumRiskScore))
                .andExpect(jsonPath("$.riskLevel").value(RiskLevel.CRITICAL.name()))
                .andExpect(jsonPath("$.matchedRules[*].ruleCode", containsInAnyOrder(
                        HighValueTransactionRule.RULE_CODE,
                        RiskyMerchantCategoryRule.RULE_CODE,
                        SuspiciousMerchantRule.RULE_CODE)));
    }

    @Test
    void invalidRequestWithCorrectScopeReturnsBadRequest()
            throws Exception
    {
        mockMvc.perform(post(transactionsEvaluatePath)
                        .with(scope(transactionEvaluateScope))
                        .contentType(APPLICATION_JSON)
                        .content(EMPTY_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(VALIDATION_FAILED_CODE));
    }

    @Test
    void missingTokenReturnsUnauthorized()
            throws Exception
    {
        mockMvc.perform(get(fraudAlertsPath))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AUTHENTICATION_REQUIRED_CODE));
    }

    @Test
    void invalidTokenReturnsUnauthorized()
            throws Exception
    {
        when(jwtDecoder.decode(INVALID_TOKEN)).thenThrow(new BadJwtException("Invalid token"));

        mockMvc.perform(get(fraudAlertsPath)
                        .header(AUTHORIZATION, BEARER_PREFIX + INVALID_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AUTHENTICATION_REQUIRED_CODE));
    }

    @Test
    void wrongScopeReturnsForbidden()
            throws Exception
    {
        mockMvc.perform(post(transactionsEvaluatePath)
                        .with(scope(fraudAlertsReadScope))
                        .contentType(APPLICATION_JSON)
                        .content(highRiskTransactionRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ACCESS_DENIED_CODE));
    }

    @Test
    void correctScopeSucceeds()
            throws Exception
    {
        mockMvc.perform(post(transactionsEvaluatePath)
                        .with(scope(transactionEvaluateScope))
                        .contentType(APPLICATION_JSON)
                        .content(highRiskTransactionRequest()))
                .andExpect(status().isOk());
    }

    @Test
    void missingResourceReturnsNotFound()
            throws Exception
    {
        mockMvc.perform(get(transactionFraudEvaluationPath(MISSING_TRANSACTION_ID))
                        .with(scope(fraudAlertsReadScope)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(RESOURCE_NOT_FOUND_CODE));
    }

    private void submitHighRiskTransaction()
            throws Exception
    {
        mockMvc.perform(post(transactionsEvaluatePath)
                        .with(scope(transactionEvaluateScope))
                        .contentType(APPLICATION_JSON)
                        .content(highRiskTransactionRequest()))
                .andExpect(status().isOk());
    }

    private String highRiskTransactionRequest()
            throws JsonProcessingException
    {
        return objectMapper.writeValueAsString(Map.copyOf(highRiskTransactionRequestValues()));
    }

    private static Map<String, Object> highRiskTransactionRequestValues()
    {
        var request = new LinkedHashMap<String, Object>();
        request.put("eventId", EVENT_ID);
        request.put("transactionId", TRANSACTION_ID);
        request.put("customerId", CUSTOMER_ID);
        request.put("accountId", ACCOUNT_ID);
        request.put("amount", HIGH_RISK_AMOUNT);
        request.put("currency", CURRENCY);
        request.put("transactionTimestamp", TRANSACTION_TIME);
        request.put("merchantCategory", MERCHANT_CATEGORY);
        request.put("country", COUNTRY);
        request.put("channel", CHANNEL);
        request.put("merchantId", MERCHANT_ID);
        request.put("merchantName", MERCHANT_NAME);
        request.put("deviceId", DEVICE_ID);

        return request;
    }

    private RequestPostProcessor scope(String scope)
    {
        return jwt().authorities(new SimpleGrantedAuthority(scopeAuthorityPrefix + scope));
    }

    private String transactionFraudEvaluationPath(String transactionId)
    {
        return transactionFraudEvaluationPath.replace(TRANSACTION_ID_PLACEHOLDER, transactionId);
    }

    @TestConfiguration
    static class FixedClockConfiguration
    {

        @Bean
        @Primary
        Clock fixedClock()
        {
            return Clock.fixed(EVALUATED_AT, ZoneOffset.UTC);
        }
    }
}
