package com.capitec.fraud.infrastructure.security;

import com.capitec.fraud.infrastructure.config.FraudSecurityProperties;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("fraudSecurityAuthorizer")
public class FraudSecurityAuthorizer
{

    private final FraudSecurityProperties properties;

    public FraudSecurityAuthorizer(FraudSecurityProperties properties)
    {
        this.properties = properties;
    }

    public boolean canEvaluateTransactions(Authentication authentication)
    {
        return hasAuthority(authentication, properties.transactionEvaluateAuthority());
    }

    public boolean canReadFraudAlerts(Authentication authentication)
    {
        return hasAuthority(authentication, properties.fraudAlertsReadAuthority());
    }

    public boolean canReadRules(Authentication authentication)
    {
        return hasAuthority(authentication, properties.rulesReadAuthority());
    }

    public boolean canAdminRules(Authentication authentication)
    {
        return hasAuthority(authentication, properties.rulesAdminAuthority());
    }

    private static boolean hasAuthority(Authentication authentication, String authority)
    {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities()
                        .stream()
                        .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
