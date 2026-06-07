package com.capitec.fraud.api.dto;

import com.capitec.fraud.api.validation.IsoCurrencyCode;
import com.capitec.fraud.api.validation.MerchantCategoryCode;
import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categorized transaction event submitted for fraud evaluation.")
public record TransactionEvaluationRequest(
        @Schema(description = "Unique event id used for idempotency.")
        @NotBlank String eventId,
        @Schema(description = "Business transaction id.")
        @NotBlank String transactionId,
        @Schema(description = "Customer identifier required for fraud evaluation.")
        @NotBlank String customerId,
        @Schema(description = "Account identifier required for fraud evaluation.")
        @NotBlank String accountId,
        @Schema(description = "Positive transaction amount.")
        @NotNull @Positive BigDecimal amount,
        @Schema(description = "ISO currency code.")
        @NotBlank @IsoCurrencyCode String currency,
        @Schema(description = "Timestamp when the transaction occurred.")
        @NotNull Instant transactionTimestamp,
        @Schema(description = "Normalized merchant category code.")
        @NotBlank @MerchantCategoryCode String merchantCategory,
        @Schema(description = "Optional transaction country code.")
        String country,
        @Schema(description = "Optional transaction channel.")
        String channel,
        @Schema(description = "Optional merchant identifier.")
        String merchantId,
        @Schema(description = "Optional merchant display name.")
        String merchantName,
        @Schema(description = "Optional device identifier.")
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
