package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.TransactionCategory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.rules.risky-merchant-category")
public record RiskyMerchantCategoryRuleProperties(
        @NotEmpty List<@NotBlank String> riskCategories,
        @NotNull @PositiveOrZero Integer defaultScore,
        @NotNull RiskLevel severity)
{

    Set<String> normalizedRiskyCategories()
    {
        return riskCategories.stream()
                .map(category -> TransactionCategory.of(category).code())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    RiskScore toRiskScore()
    {
        return RiskScore.of(defaultScore);
    }
}
