package com.capitec.fraud.infrastructure.security;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capitec.fraud.infrastructure.config.FraudSecurityConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigTest.SecurityTestController.class)
@Import({
    FraudSecurityConfiguration.class,
    FraudSecurityAuthorizer.class,
    SecurityConfig.class,
    SecurityConfigTest.SecurityTestController.class
})
@TestPropertySource(properties = {
    "fraud.security.public-paths=/actuator/health,/actuator/health/**",
    "fraud.security.transaction-evaluate-paths=/api/v1/transaction-evaluations,/api/v1/transactions/evaluate",
    "fraud.security.fraud-alerts-read-paths=/api/v1/fraud-alerts,/api/v1/fraud-alerts/**,/api/v1/transactions/*/fraud-evaluation",
    "fraud.security.rules-read-paths=/api/v1/rules,/api/v1/rules/**",
    "fraud.security.rules-admin-paths=/api/v1/rules/admin,/api/v1/rules/admin/**",
    "fraud.security.actuator-read-paths=/actuator/info",
    "fraud.security.transaction-evaluate-scope=transactions:evaluate",
    "fraud.security.fraud-alerts-read-scope=fraud-alerts:read",
    "fraud.security.rules-read-scope=rules:read",
    "fraud.security.rules-admin-scope=rules:admin",
    "fraud.security.actuator-read-scope=actuator:read",
    "fraud.security.scope-authority-prefix=SCOPE_",
    "fraud.security.jwt.issuer-uri=fraud-rule-engine-local",
    "fraud.security.jwt.audiences=fraud-rule-engine-service",
    "fraud.security.jwt.jwk-set-uri=",
    "fraud.security.jwt.public-key-location=classpath:security/local-dev-public-key.pem"
})
class SecurityConfigTest
{

    private static final String TRANSACTION_EVALUATE_AUTHORITY = "SCOPE_transactions:evaluate";
    private static final String FRAUD_ALERTS_READ_AUTHORITY = "SCOPE_fraud-alerts:read";
    private static final String RULES_READ_AUTHORITY = "SCOPE_rules:read";
    private static final String RULES_ADMIN_AUTHORITY = "SCOPE_rules:admin";
    private static final String ACTUATOR_READ_AUTHORITY = "SCOPE_actuator:read";
    private static final String OTHER_AUTHORITY = "SCOPE_other";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void requestWithoutTokenReturnsUnauthorized()
            throws Exception
    {
        mockMvc.perform(get("/api/v1/fraud-alerts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBearerTokenReturnsUnauthorized()
            throws Exception
    {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid token"));

        mockMvc.perform(get("/api/v1/fraud-alerts")
                        .header(AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtWithFraudAlertsReadScopeCanReadAlerts()
            throws Exception
    {
        mockMvc.perform(get("/api/v1/fraud-alerts")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FRAUD_ALERTS_READ_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtWithoutFraudAlertsReadScopeCannotReadAlerts()
            throws Exception
    {
        mockMvc.perform(get("/api/v1/fraud-alerts")
                        .with(jwt().authorities(new SimpleGrantedAuthority(OTHER_AUTHORITY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validJwtWithFraudAlertsReadScopeCanReadStoredEvaluation()
            throws Exception
    {
        mockMvc.perform(get("/api/v1/transactions/tx-1/fraud-evaluation")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FRAUD_ALERTS_READ_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtWithTransactionEvaluateScopeCanEvaluateTransactions()
            throws Exception
    {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .with(jwt().authorities(new SimpleGrantedAuthority(TRANSACTION_EVALUATE_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtWithoutTransactionEvaluateScopeCannotEvaluateTransactions()
            throws Exception
    {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FRAUD_ALERTS_READ_AUTHORITY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validJwtWithRulesReadScopeCanReadRuleCatalog()
            throws Exception
    {
        mockMvc.perform(get("/api/v1/rules")
                        .with(jwt().authorities(new SimpleGrantedAuthority(RULES_READ_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtWithoutRulesReadScopeCannotReadRuleCatalog()
            throws Exception
    {
        mockMvc.perform(get("/api/v1/rules")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FRAUD_ALERTS_READ_AUTHORITY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validJwtWithRulesAdminScopeCanUseRuleAdminEndpoint()
            throws Exception
    {
        mockMvc.perform(post("/api/v1/rules/admin/refresh")
                        .with(jwt().authorities(new SimpleGrantedAuthority(RULES_ADMIN_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtWithoutRulesAdminScopeCannotUseRuleAdminEndpoint()
            throws Exception
    {
        mockMvc.perform(post("/api/v1/rules/admin/refresh")
                        .with(jwt().authorities(new SimpleGrantedAuthority(RULES_READ_AUTHORITY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void actuatorDetailsRequireActuatorReadScope()
            throws Exception
    {
        mockMvc.perform(get("/actuator/info")
                        .with(jwt().authorities(new SimpleGrantedAuthority(ACTUATOR_READ_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorDetailsRejectWrongScope()
            throws Exception
    {
        mockMvc.perform(get("/actuator/info")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FRAUD_ALERTS_READ_AUTHORITY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void methodSecurityRejectsAuthenticatedRequestWithoutConfiguredScope()
            throws Exception
    {
        mockMvc.perform(get("/internal/method-only/fraud-alerts")
                        .with(jwt().authorities(new SimpleGrantedAuthority(OTHER_AUTHORITY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void methodSecurityAllowsAuthenticatedRequestWithConfiguredScope()
            throws Exception
    {
        mockMvc.perform(get("/internal/method-only/fraud-alerts")
                        .with(jwt().authorities(new SimpleGrantedAuthority(FRAUD_ALERTS_READ_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void publicPathsDoNotRequireTokens()
            throws Exception
    {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @RestController
    static class SecurityTestController
    {

        @GetMapping("/api/v1/fraud-alerts")
        @PreAuthorize("@fraudSecurityAuthorizer.canReadFraudAlerts(authentication)")
        ResponseEntity<Void> alerts()
        {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/v1/transactions/{transactionId}/fraud-evaluation")
        @PreAuthorize("@fraudSecurityAuthorizer.canReadFraudAlerts(authentication)")
        ResponseEntity<Void> storedEvaluation()
        {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/v1/transactions/evaluate")
        @PreAuthorize("@fraudSecurityAuthorizer.canEvaluateTransactions(authentication)")
        ResponseEntity<Void> evaluate()
        {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/v1/rules")
        @PreAuthorize("@fraudSecurityAuthorizer.canReadRules(authentication)")
        ResponseEntity<Void> rules()
        {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/v1/rules/admin/refresh")
        @PreAuthorize("@fraudSecurityAuthorizer.canAdminRules(authentication)")
        ResponseEntity<Void> ruleAdmin()
        {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/actuator/info")
        ResponseEntity<Void> actuatorInfo()
        {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/internal/method-only/fraud-alerts")
        @PreAuthorize("@fraudSecurityAuthorizer.canReadFraudAlerts(authentication)")
        ResponseEntity<Void> methodOnlyAlerts()
        {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/actuator/health/readiness")
        ResponseEntity<Void> health()
        {
            return ResponseEntity.ok().build();
        }
    }
}
