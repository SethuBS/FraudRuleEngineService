package com.capitec.fraud.rules;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.Transaction;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

public class VelocityTransactionRule implements FraudRule
{

    public static final String RULE_CODE = "VELOCITY_TRANSACTION";

    private static final String RULE_NAME = "Velocity Transaction";
    private static final String RULE_DESCRIPTION =
            "Flags customers or accounts with too many transactions in the configured time window";
    private static final String MATCHED_REASON_TEMPLATE =
            "Observed %d transactions within %d minutes, exceeding velocity threshold %d";
    private static final String NOT_MATCHED_REASON_TEMPLATE =
            "Observed %d transactions within %d minutes, within velocity threshold %d";
    private static final int EQUAL_COMPARISON = BigDecimal.ZERO.signum();

    private final int transactionCountThreshold;
    private final Duration timeWindow;
    private final RiskScore defaultScore;
    private final RiskLevel severity;

    public VelocityTransactionRule(
            int transactionCountThreshold,
            Duration timeWindow,
            RiskScore defaultScore,
            RiskLevel severity)
    {
        this.transactionCountThreshold = requirePositive(transactionCountThreshold, "transactionCountThreshold");
        this.timeWindow = requirePositive(timeWindow);
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
        var observedCount = observedTransactionCount(context);

        if (observedCount > transactionCountThreshold)
        {
            return RuleMatch.matched(reason(MATCHED_REASON_TEMPLATE, observedCount));
        }

        return RuleMatch.notMatched(reason(NOT_MATCHED_REASON_TEMPLATE, observedCount));
    }

    private long observedTransactionCount(TransactionContext context)
    {
        var currentTransaction = context.transaction();

        return Stream.concat(
                        Stream.of(currentTransaction),
                        context.recentTransactions(timeWindow).stream()
                                .filter(historicalTransaction -> isNotCurrentTransaction(
                                        currentTransaction,
                                        historicalTransaction)))
                .map(Transaction::transactionId)
                .distinct()
                .count();
    }

    private boolean isNotCurrentTransaction(
            Transaction currentTransaction,
            Transaction historicalTransaction)
    {
        return !currentTransaction.transactionId().equals(historicalTransaction.transactionId());
    }

    private String reason(String template, long observedCount)
    {
        return template.formatted(
                observedCount,
                timeWindow.toMinutes(),
                transactionCountThreshold);
    }

    private static int requirePositive(int value, String fieldName)
    {
        if (Integer.compare(value, EQUAL_COMPARISON) <= EQUAL_COMPARISON)
        {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }

        return value;
    }

    private static Duration requirePositive(Duration value)
    {
        var duration = Objects.requireNonNull(value, "timeWindow is required");

        if (duration.compareTo(Duration.ZERO) <= EQUAL_COMPARISON)
        {
            throw new IllegalArgumentException("timeWindow must be greater than zero");
        }

        return duration;
    }
}
