package com.capitec.fraud.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAudienceValidatorTest
{

    private static final String ACCEPTED_AUDIENCE = "fraud-rule-engine-service";
    private static final String OTHER_AUDIENCE = "other-service";

    @Test
    void acceptsTokenWhenAudienceMatchesConfiguredAudience()
    {
        var validator = new JwtAudienceValidator(List.of(ACCEPTED_AUDIENCE));

        var result = validator.validate(jwtWithAudience(ACCEPTED_AUDIENCE));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenWhenAudienceDoesNotMatchConfiguredAudience()
    {
        var validator = new JwtAudienceValidator(List.of(ACCEPTED_AUDIENCE));

        var result = validator.validate(jwtWithAudience(OTHER_AUDIENCE));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void acceptsAnyAudienceWhenNoAudienceIsConfigured()
    {
        var validator = new JwtAudienceValidator(List.of());

        var result = validator.validate(jwtWithAudience(OTHER_AUDIENCE));

        assertThat(result.hasErrors()).isFalse();
    }

    private static Jwt jwtWithAudience(String audience)
    {
        var now = Instant.now();

        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .audience(List.of(audience))
                .build();
    }
}
