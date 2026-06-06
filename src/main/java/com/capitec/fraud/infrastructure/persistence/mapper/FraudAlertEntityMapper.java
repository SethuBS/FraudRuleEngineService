package com.capitec.fraud.infrastructure.persistence.mapper;

import com.capitec.fraud.domain.FraudAlert;
import com.capitec.fraud.infrastructure.config.FraudPersistenceProperties;
import com.capitec.fraud.infrastructure.persistence.entity.FraudAlertEntity;
import com.capitec.fraud.infrastructure.persistence.entity.TransactionEntity;

import org.springframework.stereotype.Component;

@Component
public class FraudAlertEntityMapper
{

    private final FraudPersistenceProperties properties;

    public FraudAlertEntityMapper(FraudPersistenceProperties properties)
    {
        this.properties = properties;
    }

    public FraudAlertEntity toEntity(FraudAlert alert, TransactionEntity transaction)
    {
        return new FraudAlertEntity(
                alert.alertId(),
                transaction,
                alert.transaction().customerId(),
                alert.transaction().accountId(),
                alert.decision(),
                alert.riskScore().value(),
                alert.riskLevel(),
                properties.defaultAlertStatus());
    }
}
