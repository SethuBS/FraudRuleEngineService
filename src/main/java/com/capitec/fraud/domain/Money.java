package com.capitec.fraud.domain;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency)
{

    public Money
    {
        amount = DomainValidation.requirePositive(amount, "amount");
        currency = DomainValidation.requirePresent(currency, "currency");
    }

    public static Money of(BigDecimal amount, String currencyCode)
    {
        return new Money(amount, DomainValidation.requireCurrency(currencyCode, "currency"));
    }

    public String currencyCode()
    {
        return currency.getCurrencyCode();
    }
}
