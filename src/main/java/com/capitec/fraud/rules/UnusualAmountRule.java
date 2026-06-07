package com.capitec.fraud.rules;

import com.capitec.fraud.domain.Money;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public class UnusualAmountRule implements FraudRule
{

    public static final String RULE_CODE = "UNUSUAL_AMOUNT";

    private static final String RULE_NAME = "Unusual Amount";
    private static final String RULE_DESCRIPTION =
            "Flags transactions whose amount significantly exceeds the historical average";
    private static final String MATCHED_REASON_TEMPLATE =
            "Current amount %s %s exceeds %sx historical average amount %s %s";
    private static final String NOT_MATCHED_REASON_TEMPLATE =
            "Current amount %s %s is within %sx historical average amount %s %s";
    private static final String NO_BASELINE_REASON_TEMPLATE =
            "No historical average amount baseline exists for customer/account in %s";
    private static final int EQUAL_COMPARISON = BigDecimal.ZERO.signum();

    private final BigDecimal multiplierThreshold;
    private final RiskScore defaultScore;
    private final RiskLevel severity;

    public UnusualAmountRule(
            BigDecimal multiplierThreshold,
            RiskScore defaultScore,
            RiskLevel severity)
    {
        this.multiplierThreshold = requirePositive(multiplierThreshold);
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
        var transactionAmount = context.transaction().amount();
        var averageAmount = context.historicalAverageAmount();

        if (averageAmount.isEmpty())
        {
            return RuleMatch.notMatched(NO_BASELINE_REASON_TEMPLATE.formatted(transactionAmount.currencyCode()));
        }

        if (exceedsMultiplier(transactionAmount, averageAmount))
        {
            return RuleMatch.matched(reason(MATCHED_REASON_TEMPLATE, transactionAmount, averageAmount));
        }

        return RuleMatch.notMatched(reason(NOT_MATCHED_REASON_TEMPLATE, transactionAmount, averageAmount));
    }

    private boolean exceedsMultiplier(
            Money transactionAmount,
            Optional<Money> averageAmount)
    {
        var thresholdAmount = averageAmount.orElseThrow().amount().multiply(multiplierThreshold);

        return transactionAmount.amount().compareTo(thresholdAmount) > EQUAL_COMPARISON;
    }

    private String reason(
            String template,
            Money transactionAmount,
            Optional<Money> averageAmount)
    {
        var average = averageAmount.orElseThrow();

        return template.formatted(
                transactionAmount.amount().toPlainString(),
                transactionAmount.currencyCode(),
                multiplierThreshold.toPlainString(),
                average.amount().toPlainString(),
                average.currencyCode());
    }

    private static BigDecimal requirePositive(BigDecimal value)
    {
        var amount = Objects.requireNonNull(value, "multiplierThreshold is required");

        if (amount.compareTo(BigDecimal.ZERO) <= EQUAL_COMPARISON)
        {
            throw new IllegalArgumentException("multiplierThreshold must be greater than zero");
        }

        return amount;
    }
}
