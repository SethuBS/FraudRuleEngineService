package com.capitec.fraud.infrastructure.persistence;

import com.capitec.fraud.application.FraudRuleEnablement;
import com.capitec.fraud.infrastructure.config.FraudRuleCatalogProperties;
import com.capitec.fraud.infrastructure.persistence.entity.FraudRuleEntity;
import com.capitec.fraud.infrastructure.persistence.repository.FraudRuleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseFraudRuleEnablement implements FraudRuleEnablement
{

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudRuleCatalogProperties properties;

    public DatabaseFraudRuleEnablement(
            FraudRuleRepository fraudRuleRepository,
            FraudRuleCatalogProperties properties)
    {
        this.fraudRuleRepository = fraudRuleRepository;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(String ruleCode)
    {
        return fraudRuleRepository.findByCode(ruleCode)
                .map(FraudRuleEntity::isEnabled)
                .orElse(properties.enabledByDefault());
    }
}
