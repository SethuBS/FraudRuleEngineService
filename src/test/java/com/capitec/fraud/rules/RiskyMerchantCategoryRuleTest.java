package com.capitec.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class RiskyMerchantCategoryRuleTest
{

    private static final RiskScore DEFAULT_SCORE = RiskScore.of(30);
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");

    private final RiskyMerchantCategoryRule rule = new RiskyMerchantCategoryRule(
            List.of("GAMBLING", "CRYPTO", "JEWELRY"),
            DEFAULT_SCORE,
            RiskLevel.MEDIUM);

    @Test
    void riskyMerchantCategoryMatches()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("JEWELRY")));

        assertThat(result.matched()).isTrue();
        assertThat(result.explanation())
                .contains("Merchant category JEWELRY")
                .contains("configured as risky");
    }

    @Test
    void normalMerchantCategoryDoesNotMatch()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("GROCERY")));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("Merchant category GROCERY")
                .contains("not configured as risky");
    }

    @Test
    void merchantCategoryMatchingIsCaseInsensitive()
    {
        var caseInsensitiveRule = new RiskyMerchantCategoryRule(
                List.of("gambling", "crypto"),
                DEFAULT_SCORE,
                RiskLevel.MEDIUM);

        var result = caseInsensitiveRule.evaluate(TransactionContext.current(sampleTransaction("crypto")));

        assertThat(result.matched()).isTrue();
        assertThat(result.explanation())
                .contains("Merchant category CRYPTO")
                .contains("configured as risky");
    }

    @Test
    void exposesConfiguredMetadata()
    {
        assertThat(rule.code()).isEqualTo(RiskyMerchantCategoryRule.RULE_CODE);
        assertThat(rule.name()).isEqualTo("Risky Merchant Category");
        assertThat(rule.description()).contains("configured risky category list");
        assertThat(rule.defaultScore()).isEqualTo(DEFAULT_SCORE);
        assertThat(rule.severity()).isEqualTo(RiskLevel.MEDIUM);
    }

    private static Transaction sampleTransaction(String merchantCategory)
    {
        return new Transaction(
                "event-1",
                "tx-1",
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of(merchantCategory),
                TRANSACTION_TIME,
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }
}
