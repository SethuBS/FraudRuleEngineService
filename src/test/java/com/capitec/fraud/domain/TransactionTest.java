package com.capitec.fraud.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class TransactionTest
{

    @Test
    void createsNormalizedTransactionWithoutSensitiveCardFields()
    {
        var transaction = sampleTransaction();

        assertThat(transaction.eventId()).isEqualTo("event-1");
        assertThat(transaction.transactionId()).isEqualTo("tx-1");
        assertThat(transaction.category().code()).isEqualTo("GROCERY");
        assertThat(transaction.amount().amount()).isEqualByComparingTo("100.50");
        assertThat(transaction.amount().currencyCode()).isEqualTo("ZAR");
        assertThat(transaction.channel()).isEqualTo("MOBILE");
        assertThat(transaction.country()).isEqualTo("ZA");

        assertThat(Arrays.stream(Transaction.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .map(String::toLowerCase))
                .noneMatch(name -> name.contains("pan") || name.contains("card"));
    }

    @Test
    void rejectsMissingRequiredIdentifiers()
    {
        assertThatThrownBy(() -> new Transaction(
                " ",
                "tx-1",
                "customer-1",
                "account-1",
                Money.of(new BigDecimal("100.00"), "ZAR"),
                TransactionCategory.of("grocery"),
                Instant.parse("2026-06-07T08:00:00Z"),
                null,
                null,
                null,
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventId");
    }

    @Test
    void rejectsNonPositiveAmounts()
    {
        assertThatThrownBy(() -> Money.of(BigDecimal.ZERO, "ZAR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void rejectsInvalidCurrencyCodes()
    {
        assertThatThrownBy(() -> Money.of(new BigDecimal("100.00"), "ZA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("three-letter ISO currency code");
    }

    static Transaction sampleTransaction()
    {
        return new Transaction(
                " event-1 ",
                " tx-1 ",
                " customer-1 ",
                " account-1 ",
                Money.of(new BigDecimal("100.50"), "zar"),
                TransactionCategory.of("grocery"),
                Instant.parse("2026-06-07T08:00:00Z"),
                "merchant-1",
                "Corner Shop",
                "mobile",
                "za",
                "device-1");
    }
}
