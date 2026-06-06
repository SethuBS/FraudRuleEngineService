package com.capitec.fraud.infrastructure.config;

import java.time.ZoneId;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.time")
public record ApplicationClockProperties(@NotBlank String zoneId)
{

    public ZoneId toZoneId()
    {
        return ZoneId.of(zoneId);
    }
}
