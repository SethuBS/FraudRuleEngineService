package com.capitec.fraud.rules;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.TransactionCategory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class RiskyMerchantCategoryRule implements FraudRule
{

    public static final String RULE_CODE = "RISKY_MERCHANT_CATEGORY";

    private static final String RULE_NAME = "Risky Merchant Category";
    private static final String RULE_DESCRIPTION =
            "Flags transactions whose merchant category is in the configured risky category list";
    private static final String MATCHED_REASON_TEMPLATE =
            "Merchant category %s is configured as risky";
    private static final String NOT_MATCHED_REASON_TEMPLATE =
            "Merchant category %s is not configured as risky";

    private final Set<String> riskyCategories;
    private final RiskScore defaultScore;
    private final RiskLevel severity;

    public RiskyMerchantCategoryRule(
            Collection<String> riskyCategories,
            RiskScore defaultScore,
            RiskLevel severity)
    {
        this.riskyCategories = normalizedCategories(riskyCategories);
        this.defaultScore = Objects.requireNonNull(defaultScore, "defaultScore is required");
        this.severity = Objects.requireNonNull(severity, "severity is required");
    }

    @Override
    public String code()
    {
        return RULE_CODE;
    }

    @Override
    public String name()
    {
        return RULE_NAME;
    }

    @Override
    public String description()
    {
        return RULE_DESCRIPTION;
    }

    @Override
    public RiskScore defaultScore()
    {
        return defaultScore;
    }

    @Override
    public RiskLevel severity()
    {
        return severity;
    }

    @Override
    public RuleMatch evaluate(TransactionContext context)
    {
        var category = normalizedCategory(context.transaction().category());

        if (riskyCategories.contains(category))
        {
            return RuleMatch.matched(MATCHED_REASON_TEMPLATE.formatted(category));
        }

        return RuleMatch.notMatched(NOT_MATCHED_REASON_TEMPLATE.formatted(category));
    }

    private static Set<String> normalizedCategories(Collection<String> categories)
    {
        var normalizedCategories = new LinkedHashSet<String>();

        for (var category : Objects.requireNonNull(categories, "riskyCategories is required"))
        {
            normalizedCategories.add(normalizedCategory(category));
        }

        if (normalizedCategories.isEmpty())
        {
            throw new IllegalArgumentException("riskyCategories is required");
        }

        return Set.copyOf(normalizedCategories);
    }

    private static String normalizedCategory(TransactionCategory category)
    {
        return Objects.requireNonNull(category, "transactionCategory is required")
                .code()
                .strip()
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizedCategory(String category)
    {
        return TransactionCategory.of(category).code();
    }
}
