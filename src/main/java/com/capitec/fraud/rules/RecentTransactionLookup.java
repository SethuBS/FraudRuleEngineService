package com.capitec.fraud.rules;

import com.capitec.fraud.domain.Transaction;

import java.time.Duration;
import java.util.List;

@FunctionalInterface
public interface RecentTransactionLookup
{

    List<Transaction> recentTransactions(Transaction transaction, Duration timeWindow);

    static RecentTransactionLookup empty()
    {
        return (transaction, timeWindow) -> List.of();
    }
}
