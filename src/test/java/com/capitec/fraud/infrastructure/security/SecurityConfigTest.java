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
    SecurityConfig.class,
    SecurityConfigTest.SecurityTestController.class
})
@TestPropertySource(properties = {
    "fraud.security.public-paths=/actuator/health/**",
    "fraud.security.read-paths=/api/v1/fraud-alerts,/api/v1/fraud-alerts/**,/api/v1/transactions/*/fraud-evaluation",
    "fraud.security.write-paths=/api/v1/transaction-evaluations,/api/v1/transactions/evaluate",
    "fraud.security.read-scope=fraud.read",
    "fraud.security.write-scope=fraud.evaluate",
    "fraud.security.scope-authority-prefix=SCOPE_",
    "fraud.security.jwt.issuer-uri=fraud-rule-engine-local",
    "fraud.security.jwt.audiences=fraud-rule-engine-service",
    "fraud.security.jwt.jwk-set-uri=",
    "fraud.security.jwt.public-key-location=classpath:security/local-dev-public-key.pem"
})
class SecurityConfigTest
{

    private static final String READ_AUTHORITY = "SCOPE_fraud.read";
    private static final String WRITE_AUTHORITY = "SCOPE_fraud.evaluate";
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
    void validJwtWithReadScopeCanReadAlerts()
            throws Exception
    {
        mockMvc.perform(get("/api/v1/fraud-alerts")
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtWithoutReadScopeCannotReadAlerts()
            throws Exception
    {
        mockMvc.perform(get("/api/v1/fraud-alerts")
                        .with(jwt().authorities(new SimpleGrantedAuthority(OTHER_AUTHORITY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validJwtWithWriteScopeCanEvaluateTransactions()
            throws Exception
    {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .with(jwt().authorities(new SimpleGrantedAuthority(WRITE_AUTHORITY))))
                .andExpect(status().isOk());
    }

    @Test
    void validJwtWithoutWriteScopeCannotEvaluateTransactions()
            throws Exception
    {
        mockMvc.perform(post("/api/v1/transactions/evaluate")
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ_AUTHORITY))))
                .andExpect(status().isForbidden());
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
        ResponseEntity<Void> alerts()
        {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/v1/transactions/evaluate")
        ResponseEntity<Void> evaluate()
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
