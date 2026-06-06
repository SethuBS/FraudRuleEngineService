package com.capitec.fraud.domain;

import java.time.Instant;

public record Transaction(
        String eventId,
        String transactionId,
        String customerId,
        String accountId,
        Money amount,
        TransactionCategory category,
        Instant transactionTime,
        String merchantId,
        String merchantName,
        String channel,
        String country,
        String deviceId)
{

    public Transaction
    {
        eventId = DomainValidation.requireNonBlank(eventId, "eventId");
        transactionId = DomainValidation.requireNonBlank(transactionId, "transactionId");
        customerId = DomainValidation.requireNonBlank(customerId, "customerId");
        accountId = DomainValidation.requireNonBlank(accountId, "accountId");
        amount = DomainValidation.requirePresent(amount, "amount");
        category = DomainValidation.requirePresent(category, "transactionCategory");
        transactionTime = DomainValidation.requireInstant(transactionTime, "transactionTime");
        merchantId = DomainValidation.optionalText(merchantId);
        merchantName = DomainValidation.optionalText(merchantName);
        channel = DomainValidation.optionalUppercaseCode(channel);
        country = DomainValidation.optionalUppercaseCode(country);
        deviceId = DomainValidation.optionalText(deviceId);
    }
}
