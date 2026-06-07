package com.capitec.fraud.rules;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

import java.math.BigDecimal;
import java.util.Objects;

public class HighValueTransactionRule implements FraudRule
{

    public static final String RULE_CODE = "HIGH_VALUE_TRANSACTION";

    private static final String RULE_NAME = "High Value Transaction";
    private static final String RULE_DESCRIPTION = "Flags transactions above the configured high-value threshold";
    private static final String MATCHED_REASON_TEMPLATE = "Transaction amount %s %s exceeds high-value threshold %s %s";
    private static final String NOT_MATCHED_REASON_TEMPLATE = "Transaction amount %s %s is within high-value threshold %s %s";
    private static final int EQUAL_COMPARISON = BigDecimal.ZERO.signum();

    private final BigDecimal thresholdAmount;
    private final RiskScore defaultScore;
    private final RiskLevel severity;

    public HighValueTransactionRule(
            BigDecimal thresholdAmount,
            RiskScore defaultScore,
            RiskLevel severity)
    {
        this.thresholdAmount = requirePositive(thresholdAmount);
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
        var transaction = context.transaction();
        var amount = transaction.amount();

        if (amount.amount().compareTo(thresholdAmount) > EQUAL_COMPARISON)
        {
            return RuleMatch.matched(reason(MATCHED_REASON_TEMPLATE, amount.amount(), amount.currencyCode()));
        }

        return RuleMatch.notMatched(reason(NOT_MATCHED_REASON_TEMPLATE, amount.amount(), amount.currencyCode()));
    }

    private String reason(
            String template,
            BigDecimal transactionAmount,
            String currencyCode)
    {
        return template.formatted(
                transactionAmount.toPlainString(),
                currencyCode,
                thresholdAmount.toPlainString(),
                currencyCode);
    }

    private static BigDecimal requirePositive(BigDecimal value)
    {
        var amount = Objects.requireNonNull(value, "thresholdAmount is required");

        if (amount.compareTo(BigDecimal.ZERO) <= EQUAL_COMPARISON)
        {
            throw new IllegalArgumentException("thresholdAmount must be greater than zero");
        }

        return amount;
    }
}
