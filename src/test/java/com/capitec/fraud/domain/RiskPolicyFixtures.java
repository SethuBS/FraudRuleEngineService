package com.capitec.fraud.domain;

final class RiskPolicyFixtures
{

    private RiskPolicyFixtures()
    {
    }

    static RiskPolicy baselinePolicy()
    {
        return new RiskPolicy(
                RiskScore.of(25),
                RiskScore.of(50),
                RiskScore.of(75),
                RiskScore.of(100),
                RiskLevel.MEDIUM,
                RiskLevel.HIGH);
    }
}
