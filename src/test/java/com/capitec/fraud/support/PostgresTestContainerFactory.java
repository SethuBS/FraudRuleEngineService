package com.capitec.fraud.support;

import java.time.Duration;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgresTestContainerFactory
{

    private static final String IMAGE_PROPERTY = "test.postgres.image";
    private static final String IMAGE_ENVIRONMENT_VARIABLE = "TEST_POSTGRES_IMAGE";
    private static final String POSTGRES_COMPATIBLE_IMAGE = "postgres";
    private static final String STARTUP_TIMEOUT_PROPERTY = "test.postgres.startup-timeout";
    private static final String STARTUP_TIMEOUT_ENVIRONMENT_VARIABLE = "TEST_POSTGRES_STARTUP_TIMEOUT";

    private PostgresTestContainerFactory()
    {
    }

    public static PostgreSQLContainer<?> create()
    {
        return new PostgreSQLContainer<>(
                DockerImageName.parse(configuredValue(IMAGE_PROPERTY, IMAGE_ENVIRONMENT_VARIABLE))
                        .asCompatibleSubstituteFor(POSTGRES_COMPATIBLE_IMAGE))
                .withStartupTimeout(Duration.parse(configuredValue(
                        STARTUP_TIMEOUT_PROPERTY,
                        STARTUP_TIMEOUT_ENVIRONMENT_VARIABLE)));
    }

    private static String configuredValue(String propertyName, String environmentVariable)
    {
        var propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank())
        {
            return propertyValue;
        }

        var environmentValue = System.getenv(environmentVariable);
        if (environmentValue != null && !environmentValue.isBlank())
        {
            return environmentValue;
        }

        throw new IllegalStateException(
                "Configure " + propertyName + " or " + environmentVariable + " before running PostgreSQL integration tests");
    }
}
