package com.capitec.fraud.infrastructure.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.capitec.fraud.infrastructure.config.FraudSecurityProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig
{

    private final FraudSecurityProperties properties;
    private final ResourceLoader resourceLoader;

    public SecurityConfig(
            FraudSecurityProperties properties,
            ResourceLoader resourceLoader)
    {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiSecurityErrorHandler apiSecurityErrorHandler)
            throws Exception
    {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(properties.publicPathMatchers()).permitAll()
                        .requestMatchers(properties.transactionEvaluatePathMatchers())
                        .hasAuthority(properties.transactionEvaluateAuthority())
                        .requestMatchers(properties.rulesAdminPathMatchers()).hasAuthority(properties.rulesAdminAuthority())
                        .requestMatchers(properties.rulesReadPathMatchers()).hasAuthority(properties.rulesReadAuthority())
                        .requestMatchers(properties.fraudAlertsReadPathMatchers())
                        .hasAuthority(properties.fraudAlertsReadAuthority())
                        .requestMatchers(properties.actuatorReadPathMatchers()).hasAuthority(properties.actuatorReadAuthority())
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(apiSecurityErrorHandler)
                        .accessDeniedHandler(apiSecurityErrorHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(apiSecurityErrorHandler)
                        .accessDeniedHandler(apiSecurityErrorHandler)
                        .jwt(withDefaults()))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder()
    {
        var jwtProperties = properties.jwt();
        if (!properties.jwtDecoderConfigured())
        {
            return token -> {
                throw new JwtException("JWT decoder is not configured");
            };
        }

        var decoder = jwtProperties.jwkSetUriConfigured()
                ? NimbusJwtDecoder.withJwkSetUri(jwtProperties.jwkSetUri()).build()
                : NimbusJwtDecoder.withPublicKey(new RsaPublicKeyLoader(resourceLoader)
                        .load(jwtProperties.publicKeyLocation()))
                        .build();
        decoder.setJwtValidator(jwtValidator(jwtProperties));

        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> jwtValidator(FraudSecurityProperties.Jwt jwtProperties)
    {
        if (jwtProperties.issuerUriConfigured())
        {
            return new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefault(),
                    new JwtIssuerValidator(jwtProperties.issuerUri()),
                    new JwtAudienceValidator(jwtProperties.audiences()));
        }

        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtAudienceValidator(jwtProperties.audiences()));
    }
}
