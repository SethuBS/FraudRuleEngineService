package com.capitec.fraud.infrastructure.observability;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capitec.fraud.support.PostgresIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class ActuatorOperationalEndpointTest extends PostgresIntegrationTest
{

    private static final String ACTUATOR_READ_AUTHORITY = "SCOPE_actuator:read";
    private static final String OTHER_AUTHORITY = "SCOPE_other";

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = createPostgresContainer();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry)
    {
        registerDatasourceProperties(registry, POSTGRESQL);
    }

    @Test
    void healthIsPublicAndDoesNotExposeDetails()
            throws Exception
    {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void livenessProbeIsPublic()
            throws Exception
    {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readinessProbeIncludesDatabaseWhenAuthorized()
            throws Exception
    {
        mockMvc.perform(get("/actuator/health/readiness")
                        .with(jwt().authorities(new SimpleGrantedAuthority(ACTUATOR_READ_AUTHORITY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.readinessState.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void infoEndpointReturnsApplicationMetadataForActuatorReaders()
            throws Exception
    {
        mockMvc.perform(get("/actuator/info")
                        .with(jwt().authorities(new SimpleGrantedAuthority(ACTUATOR_READ_AUTHORITY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("FraudRuleEngineService"))
                .andExpect(jsonPath("$.app.description").value("Fraud Rule Engine Service API"))
                .andExpect(jsonPath("$.app.version").value("0.0.1-SNAPSHOT"));
    }

    @Test
    void metricsEndpointRequiresActuatorScope()
            throws Exception
    {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/metrics")
                        .with(jwt().authorities(new SimpleGrantedAuthority(OTHER_AUTHORITY))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/metrics")
                        .with(jwt().authorities(new SimpleGrantedAuthority(ACTUATOR_READ_AUTHORITY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void prometheusEndpointRequiresActuatorScope()
            throws Exception
    {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/prometheus")
                        .with(jwt().authorities(new SimpleGrantedAuthority(ACTUATOR_READ_AUTHORITY))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# HELP")));
    }

    @Test
    void dangerousActuatorEndpointsAreNotExposed()
            throws Exception
    {
        mockMvc.perform(get("/actuator/env")
                        .with(jwt().authorities(new SimpleGrantedAuthority(ACTUATOR_READ_AUTHORITY))))
                .andExpect(status().isNotFound());
    }
}
