package com.capitec.fraud.domain;

public enum FraudDecision
{
    APPROVED,
    REVIEW,
    FLAGGED;

    public static FraudDecision from(RiskScore riskScore, RiskPolicy riskPolicy)
    {
        DomainValidation.requirePresent(riskScore, "riskScore");
        return DomainValidation.requirePresent(riskPolicy, "riskPolicy").decisionFor(riskScore);
    }

    public boolean requiresManualReview()
    {
        return this == REVIEW || this == FLAGGED;
    }

    public boolean requiresAlert()
    {
        return this == FLAGGED;
    }
}
