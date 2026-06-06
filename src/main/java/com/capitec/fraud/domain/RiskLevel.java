package com.capitec.fraud.domain;

public enum RiskLevel
{
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean isAtLeast(RiskLevel minimumLevel)
    {
        return compareTo(DomainValidation.requirePresent(minimumLevel, "minimumLevel")) >= 0;
    }
}
