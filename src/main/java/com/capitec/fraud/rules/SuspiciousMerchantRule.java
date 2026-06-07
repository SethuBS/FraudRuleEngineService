package com.capitec.fraud.rules;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class SuspiciousMerchantRule implements FraudRule
{

    public static final String RULE_CODE = "SUSPICIOUS_MERCHANT";

    private static final String RULE_NAME = "Suspicious Merchant";
    private static final String RULE_DESCRIPTION =
            "Flags transactions involving configured suspicious merchant identifiers or names";
    private static final String MERCHANT_ID_MATCHED_REASON_TEMPLATE =
            "Merchant ID %s is configured as suspicious";
    private static final String MERCHANT_NAME_MATCHED_REASON_TEMPLATE =
            "Merchant name %s contains suspicious fragment %s";
    private static final String NOT_MATCHED_REASON =
            "Merchant ID/name did not match configured suspicious merchants";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final Set<String> suspiciousMerchantIds;
    private final Set<String> suspiciousMerchantNameFragments;
    private final RiskScore defaultScore;
    private final RiskLevel severity;

    public SuspiciousMerchantRule(
            Collection<String> suspiciousMerchantIds,
            Collection<String> suspiciousMerchantNameFragments,
            RiskScore defaultScore,
            RiskLevel severity)
    {
        this.suspiciousMerchantIds = normalizedValues(suspiciousMerchantIds);
        this.suspiciousMerchantNameFragments = normalizedValues(suspiciousMerchantNameFragments);
        this.defaultScore = Objects.requireNonNull(defaultScore, "defaultScore is required");
        this.severity = Objects.requireNonNull(severity, "severity is required");

        if (this.suspiciousMerchantIds.isEmpty() && this.suspiciousMerchantNameFragments.isEmpty())
        {
            throw new IllegalArgumentException("suspicious merchant IDs or name fragments are required");
        }
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
        var transaction = context.transaction();
        var merchantId = normalizedOptional(transaction.merchantId());

        if (merchantId.isPresent() && suspiciousMerchantIds.contains(merchantId.get()))
        {
            return RuleMatch.matched(MERCHANT_ID_MATCHED_REASON_TEMPLATE.formatted(merchantId.get()));
        }

        var merchantName = normalizedOptional(transaction.merchantName());
        var suspiciousFragment = matchingFragment(merchantName);

        if (suspiciousFragment.isPresent())
        {
            return RuleMatch.matched(MERCHANT_NAME_MATCHED_REASON_TEMPLATE.formatted(
                    merchantName.orElseThrow(),
                    suspiciousFragment.get()));
        }

        return RuleMatch.notMatched(NOT_MATCHED_REASON);
    }

    private Optional<String> matchingFragment(Optional<String> merchantName)
    {
        if (merchantName.isEmpty())
        {
            return Optional.empty();
        }

        return suspiciousMerchantNameFragments.stream()
                .filter(fragment -> merchantName.get().contains(fragment))
                .findFirst();
    }

    private static Set<String> normalizedValues(Collection<String> values)
    {
        var normalizedValues = new LinkedHashSet<String>();

        for (var value : Objects.requireNonNull(values, "configured merchant values are required"))
        {
            normalizedOptional(value).ifPresent(normalizedValues::add);
        }

        return Collections.unmodifiableSet(normalizedValues);
    }

    private static Optional<String> normalizedOptional(String value)
    {
        if (value == null)
        {
            return Optional.empty();
        }

        var normalized = WHITESPACE.matcher(value.strip()).replaceAll(" ").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }
}
