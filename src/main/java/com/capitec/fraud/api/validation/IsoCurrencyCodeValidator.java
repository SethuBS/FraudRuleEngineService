package com.capitec.fraud.api.validation;

import java.util.Currency;
import java.util.Locale;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsoCurrencyCodeValidator implements ConstraintValidator<IsoCurrencyCode, String>
{

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (value == null || value.isBlank())
        {
            return true;
        }

        try
        {
            Currency.getInstance(value.strip().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex)
        {
            return false;
        }
    }
}
