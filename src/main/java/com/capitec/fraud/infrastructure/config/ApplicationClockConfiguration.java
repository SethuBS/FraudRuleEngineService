package com.capitec.fraud.infrastructure.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApplicationClockProperties.class)
public class ApplicationClockConfiguration
{

    @Bean
    Clock applicationClock(ApplicationClockProperties properties)
    {
        return Clock.system(properties.toZoneId());
    }
}
