package com.capitec.fraud.api.dto;

import com.capitec.fraud.api.validation.IsoCurrencyCode;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionEvaluationRequest(
        @NotBlank String eventId,
        @NotBlank String transactionId,
        @NotBlank String customerId,
        @NotBlank String accountId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @IsoCurrencyCode String currency,
        @NotNull Instant transactionTimestamp,
        @NotBlank String merchantCategory,
        String country,
        String channel,
        String merchantId,
        String merchantName,
        String deviceId)
{

    public Transaction toDomain()
    {
        return new Transaction(
                eventId,
                transactionId,
                customerId,
                accountId,
                Money.of(amount, currency),
                TransactionCategory.of(merchantCategory),
                transactionTimestamp,
                merchantId,
                merchantName,
                channel,
                country,
                deviceId);
    }
}
