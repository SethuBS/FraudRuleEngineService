package com.capitec.fraud.api.validation;

import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;

public class MerchantCategoryCodeValidator implements ConstraintValidator<MerchantCategoryCode, String>
{

    private static final Pattern CATEGORY_CODE = Pattern.compile("[A-Z0-9_-]+");

    private final int merchantCategoryMaxLength;

    public MerchantCategoryCodeValidator(
            @Value("${fraud.api.validation.merchant-category-max-length}") int merchantCategoryMaxLength)
    {
        this.merchantCategoryMaxLength = merchantCategoryMaxLength;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (value == null || value.isBlank())
        {
            return true;
        }

        var normalized = value.strip().toUpperCase(Locale.ROOT);

        return normalized.length() <= merchantCategoryMaxLength && CATEGORY_CODE.matcher(normalized).matches();
    }
}
