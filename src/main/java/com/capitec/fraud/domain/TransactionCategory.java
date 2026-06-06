package com.capitec.fraud.domain;

import java.util.regex.Pattern;

public record TransactionCategory(String code)
{

    private static final int MAX_CODE_LENGTH = 80;
    private static final Pattern CATEGORY_CODE = Pattern.compile("[A-Z0-9_-]+");

    public TransactionCategory
    {
        code = DomainValidation.requireUppercaseCode(code, "transactionCategory");
        if (code.length() > MAX_CODE_LENGTH)
        {
            throw new IllegalArgumentException("transactionCategory must be at most " + MAX_CODE_LENGTH + " characters");
        }
        if (!CATEGORY_CODE.matcher(code).matches())
        {
            throw new IllegalArgumentException("transactionCategory can only contain letters, numbers, underscores, or hyphens");
        }
    }

    public static TransactionCategory of(String code)
    {
        return new TransactionCategory(code);
    }
}
