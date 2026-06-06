package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.evaluation")
public record FraudEvaluationProperties(
        @Valid @NotNull RiskThresholds riskThresholds,
        @Valid @NotNull DecisionThresholds decisionThresholds)
{

    public RiskPolicy toRiskPolicy()
    {
        return new RiskPolicy(
                RiskScore.of(riskThresholds.medium()),
                RiskScore.of(riskThresholds.high()),
                RiskScore.of(riskThresholds.critical()),
                decisionThresholds.review(),
                decisionThresholds.flagged());
    }

    public record RiskThresholds(
            @NotNull @Min(0) Integer medium,
            @NotNull @Min(0) Integer high,
            @NotNull @Min(0) Integer critical)
    {
    }

    public record DecisionThresholds(
            @NotNull RiskLevel review,
            @NotNull RiskLevel flagged)
    {
    }
}
