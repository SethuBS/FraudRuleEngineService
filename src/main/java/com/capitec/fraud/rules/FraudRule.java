package com.capitec.fraud.rules;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

public interface FraudRule
{

    String code();

    String name();

    String description();

    RiskScore defaultScore();

    RiskLevel severity();
}
