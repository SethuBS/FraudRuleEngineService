package com.capitec.fraud.infrastructure.config;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.security")
public record FraudSecurityProperties(
        @NotEmpty List<@NotBlank String> publicPaths,
        @NotEmpty List<@NotBlank String> readPaths,
        @NotEmpty List<@NotBlank String> writePaths,
        @NotBlank String readScope,
        @NotBlank String writeScope,
        @NotBlank String scopeAuthorityPrefix,
        @Valid @NotNull Jwt jwt)
{

    public String[] publicPathMatchers()
    {
        return publicPaths.toArray(String[]::new);
    }

    public String[] readPathMatchers()
    {
        return readPaths.toArray(String[]::new);
    }

    public String[] writePathMatchers()
    {
        return writePaths.toArray(String[]::new);
    }

    public String readAuthority()
    {
        return scopeAuthorityPrefix + readScope;
    }

    public String writeAuthority()
    {
        return scopeAuthorityPrefix + writeScope;
    }

    public boolean jwtDecoderConfigured()
    {
        return jwt.jwkSetUriConfigured() || jwt.publicKeyLocationConfigured();
    }

    public record Jwt(
            String issuerUri,
            List<String> audiences,
            String jwkSetUri,
            String publicKeyLocation)
    {

        public Jwt
        {
            audiences = audiences == null ? List.of() : List.copyOf(audiences);
        }

        public boolean issuerUriConfigured()
        {
            return issuerUri != null && !issuerUri.isBlank();
        }

        public boolean audiencesConfigured()
        {
            return !audiences.isEmpty();
        }

        public boolean jwkSetUriConfigured()
        {
            return jwkSetUri != null && !jwkSetUri.isBlank();
        }

        public boolean publicKeyLocationConfigured()
        {
            return publicKeyLocation != null && !publicKeyLocation.isBlank();
        }
    }
}
