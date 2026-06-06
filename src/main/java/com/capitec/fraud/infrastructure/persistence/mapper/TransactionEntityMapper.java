package com.capitec.fraud.infrastructure.persistence.mapper;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionCategory;
import com.capitec.fraud.infrastructure.persistence.entity.TransactionEntity;

import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class TransactionEntityMapper
{

    public TransactionEntity toEntity(Transaction transaction)
    {
        return toEntity(transaction, null, null);
    }

    public TransactionEntity toEntity(
            Transaction transaction,
            String sanitizedRawPayload,
            Instant rawPayloadExpiresAt)
    {
        return new TransactionEntity(
                transaction.eventId(),
                transaction.transactionId(),
                transaction.customerId(),
                transaction.accountId(),
                transaction.amount().amount(),
                transaction.amount().currencyCode(),
                transaction.transactionTime(),
                transaction.category().code(),
                transaction.country(),
                transaction.channel(),
                transaction.merchantId(),
                transaction.merchantName(),
                transaction.deviceId(),
                sanitizedRawPayload,
                rawPayloadExpiresAt);
    }

    public Transaction toDomain(TransactionEntity entity)
    {
        return new Transaction(
                entity.getEventId(),
                entity.getTransactionId(),
                entity.getCustomerId(),
                entity.getAccountId(),
                new Money(entity.getAmount(), java.util.Currency.getInstance(entity.getCurrency())),
                TransactionCategory.of(entity.getMerchantCategory()),
                entity.getTransactionTimestamp(),
                entity.getMerchantId(),
                entity.getMerchantName(),
                entity.getChannel(),
                entity.getCountry(),
                entity.getDeviceId());
    }
}
