package com.capitec.fraud.infrastructure.security;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAudienceValidator implements OAuth2TokenValidator<Jwt>
{

    private static final String INVALID_TOKEN_ERROR_CODE = "invalid_token";

    private final Set<String> acceptedAudiences;

    JwtAudienceValidator(Collection<String> acceptedAudiences)
    {
        this.acceptedAudiences = acceptedAudiences == null ? Set.of() : Set.copyOf(acceptedAudiences);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token)
    {
        if (acceptedAudiences.isEmpty())
        {
            return OAuth2TokenValidatorResult.success();
        }

        if (token.getAudience().stream().anyMatch(acceptedAudiences::contains))
        {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                INVALID_TOKEN_ERROR_CODE,
                "JWT audience is not accepted",
                null));
    }
}
