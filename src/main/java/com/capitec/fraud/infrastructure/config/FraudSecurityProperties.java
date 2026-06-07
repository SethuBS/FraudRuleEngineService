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
        @NotEmpty List<@NotBlank String> transactionEvaluatePaths,
        @NotEmpty List<@NotBlank String> fraudAlertsReadPaths,
        @NotEmpty List<@NotBlank String> rulesReadPaths,
        @NotEmpty List<@NotBlank String> rulesAdminPaths,
        @NotEmpty List<@NotBlank String> actuatorReadPaths,
        @NotBlank String transactionEvaluateScope,
        @NotBlank String fraudAlertsReadScope,
        @NotBlank String rulesReadScope,
        @NotBlank String rulesAdminScope,
        @NotBlank String actuatorReadScope,
        @NotBlank String scopeAuthorityPrefix,
        @Valid @NotNull Jwt jwt)
{

    public String[] publicPathMatchers()
    {
        return publicPaths.toArray(String[]::new);
    }

    public String[] transactionEvaluatePathMatchers()
    {
        return transactionEvaluatePaths.toArray(String[]::new);
    }

    public String[] fraudAlertsReadPathMatchers()
    {
        return fraudAlertsReadPaths.toArray(String[]::new);
    }

    public String[] rulesReadPathMatchers()
    {
        return rulesReadPaths.toArray(String[]::new);
    }

    public String[] rulesAdminPathMatchers()
    {
        return rulesAdminPaths.toArray(String[]::new);
    }

    public String[] actuatorReadPathMatchers()
    {
        return actuatorReadPaths.toArray(String[]::new);
    }

    public String transactionEvaluateAuthority()
    {
        return authority(transactionEvaluateScope);
    }

    public String fraudAlertsReadAuthority()
    {
        return authority(fraudAlertsReadScope);
    }

    public String rulesReadAuthority()
    {
        return authority(rulesReadScope);
    }

    public String rulesAdminAuthority()
    {
        return authority(rulesAdminScope);
    }

    public String actuatorReadAuthority()
    {
        return authority(actuatorReadScope);
    }

    private String authority(String scope)
    {
        return scopeAuthorityPrefix + scope;
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
