package com.capitec.fraud.infrastructure.persistence;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.rules.HistoricalAverageAmountLookup;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseHistoricalAverageAmountLookup implements HistoricalAverageAmountLookup
{

    private final TransactionRepository transactionRepository;

    public DatabaseHistoricalAverageAmountLookup(TransactionRepository transactionRepository)
    {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Money> averageAmount(Transaction transaction)
    {
        var currencyCode = transaction.amount().currencyCode();

        return transactionRepository.averageAmountByCustomerOrAccount(
                        transaction.customerId(),
                        transaction.accountId(),
                        transaction.transactionId(),
                        currencyCode)
                .map(averageAmount -> Money.of(averageAmount, currencyCode));
    }
}
