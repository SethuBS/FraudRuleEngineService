package com.capitec.fraud.domain;

public record RiskScore(int value) implements Comparable<RiskScore>
{

    public static final RiskScore ZERO = new RiskScore(0);

    public RiskScore
    {
        if (value < 0)
        {
            throw new IllegalArgumentException("riskScore must not be negative");
        }
    }

    public static RiskScore of(int value)
    {
        return new RiskScore(value);
    }

    public RiskScore plus(RiskScore other)
    {
        return new RiskScore(Math.addExact(value, DomainValidation.requirePresent(other, "other").value));
    }

    public RiskLevel riskLevel(RiskPolicy riskPolicy)
    {
        return DomainValidation.requirePresent(riskPolicy, "riskPolicy").riskLevelFor(this);
    }

    @Override
    public int compareTo(RiskScore other)
    {
        return Integer.compare(value, DomainValidation.requirePresent(other, "other").value);
    }
}
