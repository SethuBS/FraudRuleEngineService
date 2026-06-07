package com.capitec.fraud.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresIntegrationTest
{

    protected static PostgreSQLContainer<?> createPostgresContainer()
    {
        return PostgresTestContainerFactory.create();
    }

    protected static void registerDatasourceProperties(
            DynamicPropertyRegistry registry,
            PostgreSQLContainer<?> postgresql)
    {
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
    }
}
