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

class SuspiciousMerchantRuleTest
{

    private static final RiskScore DEFAULT_SCORE = RiskScore.of(50);
    private static final Instant TRANSACTION_TIME = Instant.parse("2026-06-07T08:00:00Z");

    private final SuspiciousMerchantRule rule = new SuspiciousMerchantRule(
            List.of("merchant-watchlist"),
            List.of("watchlisted", "high risk traders"),
            DEFAULT_SCORE,
            RiskLevel.HIGH);

    @Test
    void merchantIdMatchFlagsTransaction()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction(
                "MERCHANT-WATCHLIST",
                "Corner Shop")));

        assertThat(result.matched()).isTrue();
        assertThat(result.explanation())
                .contains("Merchant ID MERCHANT-WATCHLIST")
                .contains("configured as suspicious");
    }

    @Test
    void merchantNameFragmentMatchFlagsTransaction()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction(
                "merchant-safe",
                "Northern WatchListed Traders")));

        assertThat(result.matched()).isTrue();
        assertThat(result.explanation())
                .contains("Merchant name NORTHERN WATCHLISTED TRADERS")
                .contains("suspicious fragment WATCHLISTED");
    }

    @Test
    void safeMerchantDoesNotMatchWhenMerchantNameIsMissing()
    {
        var result = rule.evaluate(TransactionContext.current(sampleTransaction("merchant-safe", null)));

        assertThat(result.matched()).isFalse();
        assertThat(result.explanation())
                .contains("did not match configured suspicious merchants");
    }

    @Test
    void exposesConfiguredMetadata()
    {
        assertThat(rule.code()).isEqualTo(SuspiciousMerchantRule.RULE_CODE);
        assertThat(rule.name()).isEqualTo("Suspicious Merchant");
        assertThat(rule.description()).contains("configured suspicious merchant");
        assertThat(rule.defaultScore()).isEqualTo(DEFAULT_SCORE);
        assertThat(rule.severity()).isEqualTo(RiskLevel.HIGH);
    }

    private static Transaction sampleTransaction(
            String merchantId,
            String merchantName)
    {
        return new Transaction(
                "event-1",
                "tx-1",
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.50"), "ZAR"),
                TransactionCategory.of("grocery"),
                TRANSACTION_TIME,
                merchantId,
                merchantName,
                "mobile",
                "za",
                "device-1");
    }
}
