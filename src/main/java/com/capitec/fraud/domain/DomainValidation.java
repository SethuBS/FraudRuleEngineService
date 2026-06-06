package com.capitec.fraud.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

final class DomainValidation
{

    private static final Pattern ISO_CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

    private DomainValidation()
    {
    }

    static String requireNonBlank(String value, String fieldName)
    {
        if (value == null)
        {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        var trimmed = value.strip();
        if (trimmed.isEmpty())
        {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return trimmed;
    }

    static String requireUppercaseCode(String value, String fieldName)
    {
        return requireNonBlank(value, fieldName).toUpperCase(Locale.ROOT);
    }

    static String optionalText(String value)
    {
        if (value == null)
        {
            return null;
        }

        var trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String optionalUppercaseCode(String value)
    {
        var normalized = optionalText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    static <T> T requirePresent(T value, String fieldName)
    {
        if (value == null)
        {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value;
    }

    static Instant requireInstant(Instant value, String fieldName)
    {
        return requirePresent(value, fieldName);
    }

    static BigDecimal requirePositive(BigDecimal value, String fieldName)
    {
        requirePresent(value, fieldName);
        if (value.signum() <= 0)
        {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }

        return value;
    }

    static Currency requireCurrency(String currencyCode, String fieldName)
    {
        var normalized = requireUppercaseCode(currencyCode, fieldName);
        if (!ISO_CURRENCY_CODE.matcher(normalized).matches())
        {
            throw new IllegalArgumentException(fieldName + " must be a three-letter ISO currency code");
        }

        try
        {
            return Currency.getInstance(normalized);
        } catch (IllegalArgumentException ex)
        {
            throw new IllegalArgumentException(fieldName + " must be a valid ISO currency code", ex);
        }
    }
}
