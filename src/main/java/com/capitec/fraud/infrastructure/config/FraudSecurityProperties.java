package com.capitec.fraud.infrastructure.config;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.security")
public record FraudSecurityProperties(@NotEmpty List<@NotBlank String> publicPaths)
{
    public String[] publicPathMatchers()
    {
        return publicPaths.toArray(String[]::new);
    }
}
