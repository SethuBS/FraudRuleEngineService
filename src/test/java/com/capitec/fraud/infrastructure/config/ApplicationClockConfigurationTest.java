package com.capitec.fraud.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ApplicationClockConfigurationTest
{

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ApplicationClockConfiguration.class);

    @Test
    void exposesClockUsingConfiguredZone()
    {
        contextRunner
                .withPropertyValues("fraud.time.zone-id=Africa/Johannesburg")
                .run(context ->
                {
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Africa/Johannesburg"));
                });
    }
}
