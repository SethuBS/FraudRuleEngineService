package com.capitec.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.RuleEvaluationResult;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FraudDecisionServiceTest
{

    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T08:02:00Z");

    private final FraudDecisionService fraudDecisionService = new FraudDecisionService(baselinePolicy());

    @Test
    void noMatchedRulesResultsInApprovedLowDecision()
    {
        var evaluation = fraudDecisionService.evaluate(
                sampleTransaction(),
                List.of(RuleEvaluationResult.notMatched(
                        "HIGH_VALUE_TRANSACTION",
                        "High Value Transaction",
                        55,
                        "Amount was within threshold",
                        EVALUATED_AT)),
                EVALUATED_AT);

        assertThat(evaluation.riskScore()).isEqualTo(RiskScore.ZERO);
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(evaluation.decision()).isEqualTo(FraudDecision.APPROVED);
    }

    @ParameterizedTest
    @CsvSource({
        "0,LOW,APPROVED",
        "24,LOW,APPROVED",
        "25,MEDIUM,REVIEW",
        "49,MEDIUM,REVIEW",
        "50,HIGH,FLAGGED",
        "74,HIGH,FLAGGED",
        "75,CRITICAL,FLAGGED",
        "100,CRITICAL,FLAGGED"
    })
    void mapsConfiguredScoreBoundaries(
            int score,
            RiskLevel expectedRiskLevel,
            FraudDecision expectedDecision)
    {
        var riskScore = RiskScore.of(score);

        assertThat(fraudDecisionService.riskLevel(riskScore)).isEqualTo(expectedRiskLevel);
        assertThat(fraudDecisionService.decision(riskScore)).isEqualTo(expectedDecision);
    }

    @Test
    void scoreAboveConfiguredMaximumIsCappedBeforeLevelAndDecisionMapping()
    {
        var riskScore = RiskScore.of(125);

        assertThat(fraudDecisionService.riskLevel(riskScore)).isEqualTo(RiskLevel.CRITICAL);
        assertThat(fraudDecisionService.decision(riskScore)).isEqualTo(FraudDecision.FLAGGED);
    }

    @Test
    void multipleMatchedRulesAreSummedAndCappedAtMaximumScore()
    {
        var evaluation = fraudDecisionService.evaluate(
                sampleTransaction(),
                List.of(
                        RuleEvaluationResult.matched(
                                "HIGH_VALUE_TRANSACTION",
                                "High Value Transaction",
                                75,
                                "Amount exceeded threshold",
                                EVALUATED_AT),
                        RuleEvaluationResult.matched(
                                "VELOCITY_TRANSACTION",
                                "Velocity Transaction",
                                75,
                                "Velocity threshold met",
                                EVALUATED_AT),
                        RuleEvaluationResult.notMatched(
                                "RISKY_MERCHANT_CATEGORY",
                                "Risky Merchant Category",
                                30,
                                "Merchant category was normal",
                                EVALUATED_AT)),
                EVALUATED_AT);

        assertThat(evaluation.riskScore()).isEqualTo(RiskScore.of(100));
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(evaluation.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(evaluation.matchedRules()).hasSize(2);
    }

    @Test
    void unmatchedRulesRemainAuditableAndDoNotContributeToRiskScore()
    {
        var evaluation = fraudDecisionService.evaluate(
                sampleTransaction(),
                List.of(
                        RuleEvaluationResult.notMatched(
                                "HIGH_VALUE_TRANSACTION",
                                "High Value Transaction",
                                55,
                                "Amount was within threshold",
                                EVALUATED_AT),
                        RuleEvaluationResult.notMatched(
                                "VELOCITY_TRANSACTION",
                                "Velocity Transaction",
                                35,
                                "Velocity was normal",
                                EVALUATED_AT)),
                EVALUATED_AT);

        assertThat(evaluation.riskScore()).isEqualTo(RiskScore.ZERO);
        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(evaluation.decision()).isEqualTo(FraudDecision.APPROVED);
        assertThat(evaluation.matchedRules()).isEmpty();
        assertThat(evaluation.ruleResults())
                .hasSize(2)
                .allSatisfy(result ->
                {
                    assertThat(result.matched()).isFalse();
                    assertThat(result.effectiveScore()).isEqualTo(RiskScore.ZERO);
                });
    }

    private static RiskPolicy baselinePolicy()
    {
        return new RiskPolicy(
                RiskScore.of(25),
                RiskScore.of(50),
                RiskScore.of(75),
                RiskScore.of(100),
                RiskLevel.MEDIUM,
                RiskLevel.HIGH);
    }

    private static Transaction sampleTransaction()
    {
        return new Transaction(
                "event-1",
                "tx-1",
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                Instant.parse("2026-06-07T08:00:00Z"),
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }
}
