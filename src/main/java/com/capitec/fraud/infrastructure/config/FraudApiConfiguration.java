package com.capitec.fraud.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FraudApiPaginationProperties.class)
public class FraudApiConfiguration
{
}
