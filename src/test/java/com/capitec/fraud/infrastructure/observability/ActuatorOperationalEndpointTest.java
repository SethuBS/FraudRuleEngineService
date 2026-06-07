package com.capitec.fraud.infrastructure.observability;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

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
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class ActuatorOperationalEndpointTest
{

    private static final String ACTUATOR_READ_AUTHORITY = "SCOPE_actuator:read";
    private static final String OTHER_AUTHORITY = "SCOPE_other";
    private static final String POSTGRES_IMAGE = System.getProperty(
            "test.postgres.image",
            System.getenv().getOrDefault("TEST_POSTGRES_IMAGE", "postgres:16-alpine"));
    private static final long POSTGRES_STARTUP_TIMEOUT_MINUTES = Long.parseLong(System.getProperty(
            "test.postgres.startup-timeout-minutes",
            System.getenv().getOrDefault("TEST_POSTGRES_STARTUP_TIMEOUT_MINUTES", "2")));

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(
            DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
            .withStartupTimeout(Duration.ofMinutes(POSTGRES_STARTUP_TIMEOUT_MINUTES));

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
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
