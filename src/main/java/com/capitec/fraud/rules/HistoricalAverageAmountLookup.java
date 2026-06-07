package com.capitec.fraud.rules;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.Transaction;

import java.util.Optional;

@FunctionalInterface
public interface HistoricalAverageAmountLookup
{

    Optional<Money> averageAmount(Transaction transaction);

    static HistoricalAverageAmountLookup empty()
    {
        return transaction -> Optional.empty();
    }
}
