package com.capitec.fraud.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
    FraudRawPayloadCleanupProperties.class,
    FraudRawPayloadProperties.class
})
public class FraudRawPayloadConfiguration
{
}
