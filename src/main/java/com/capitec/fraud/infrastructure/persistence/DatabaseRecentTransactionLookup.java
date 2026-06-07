package com.capitec.fraud.infrastructure.persistence;

import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.infrastructure.persistence.mapper.TransactionEntityMapper;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;
import com.capitec.fraud.rules.RecentTransactionLookup;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseRecentTransactionLookup implements RecentTransactionLookup
{

    private final TransactionRepository transactionRepository;
    private final TransactionEntityMapper transactionEntityMapper;

    public DatabaseRecentTransactionLookup(
            TransactionRepository transactionRepository,
            TransactionEntityMapper transactionEntityMapper)
    {
        this.transactionRepository = transactionRepository;
        this.transactionEntityMapper = transactionEntityMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> recentTransactions(
            Transaction transaction,
            Duration timeWindow)
    {
        var windowStart = transaction.transactionTime().minus(timeWindow);
        var windowEnd = transaction.transactionTime();

        return transactionRepository.findRecentByCustomerOrAccount(
                        transaction.customerId(),
                        transaction.accountId(),
                        transaction.transactionId(),
                        windowStart,
                        windowEnd)
                .stream()
                .map(transactionEntityMapper::toDomain)
                .toList();
    }
}
