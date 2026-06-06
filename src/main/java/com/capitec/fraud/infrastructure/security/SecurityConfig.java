package com.capitec.fraud.infrastructure.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.capitec.fraud.infrastructure.config.FraudSecurityProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig
{

    private final FraudSecurityProperties properties;

    public SecurityConfig(FraudSecurityProperties properties)
    {
        this.properties = properties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(properties.publicPathMatchers()).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(withDefaults())
                .build();
    }
}
