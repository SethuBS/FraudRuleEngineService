package com.capitec.fraud.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class RuleEvaluationResultTest
{

    private static final Instant EVALUATED_AT = Instant.parse("2026-06-07T08:01:00Z");

    @Test
    void matchedRuleContributesToEffectiveScore()
    {
        var result = RuleEvaluationResult.matched(
                "high_amount",
                "High Amount",
                35,
                "Amount exceeded threshold",
                EVALUATED_AT);

        assertThat(result.ruleCode()).isEqualTo("HIGH_AMOUNT");
        assertThat(result.matched()).isTrue();
        assertThat(result.scoreContribution()).isEqualTo(RiskScore.of(35));
        assertThat(result.effectiveScore()).isEqualTo(RiskScore.of(35));
        assertThat(result.explanation()).isEqualTo("Amount exceeded threshold");
    }

    @Test
    void unmatchedRuleKeepsExplanationButDoesNotContributeToEffectiveScore()
    {
        var result = RuleEvaluationResult.notMatched(
                "velocity",
                "Velocity",
                25,
                "Customer transaction count stayed below threshold",
                EVALUATED_AT);

        assertThat(result.ruleCode()).isEqualTo("VELOCITY");
        assertThat(result.matched()).isFalse();
        assertThat(result.scoreContribution()).isEqualTo(RiskScore.of(25));
        assertThat(result.effectiveScore()).isEqualTo(RiskScore.ZERO);
        assertThat(result.explanation()).contains("below threshold");
    }

    @Test
    void rejectsBlankExplanations()
    {
        assertThatThrownBy(() -> RuleEvaluationResult.notMatched(
                "velocity",
                "Velocity",
                25,
                " ",
                EVALUATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("explanation");
    }
}
