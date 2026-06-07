package com.capitec.fraud.rules;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskScore;

import java.util.Locale;
import java.util.Objects;

public class ForeignCountryTransactionRule implements FraudRule
{

    public static final String RULE_CODE = "FOREIGN_COUNTRY_TRANSACTION";

    private static final String RULE_NAME = "Foreign Country Transaction";
    private static final String RULE_DESCRIPTION =
            "Flags transactions where the transaction country differs from the configured home country";
    private static final String MATCHED_REASON_TEMPLATE =
            "Transaction country %s differs from expected home country %s";
    private static final String NOT_MATCHED_REASON_TEMPLATE =
            "Transaction country %s matches expected home country %s";
    private static final String MISSING_COUNTRY_REASON_TEMPLATE =
            "Transaction country is missing; expected home country is %s";

    private final String expectedCountry;
    private final RiskScore defaultScore;
    private final RiskLevel severity;

    public ForeignCountryTransactionRule(
            String expectedCountry,
            RiskScore defaultScore,
            RiskLevel severity)
    {
        this.expectedCountry = requireCountry(expectedCountry);
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
        var transactionCountry = context.transaction().country();

        if (transactionCountry == null)
        {
            return RuleMatch.notMatched(MISSING_COUNTRY_REASON_TEMPLATE.formatted(expectedCountry));
        }

        if (!expectedCountry.equals(transactionCountry))
        {
            return RuleMatch.matched(MATCHED_REASON_TEMPLATE.formatted(transactionCountry, expectedCountry));
        }

        return RuleMatch.notMatched(NOT_MATCHED_REASON_TEMPLATE.formatted(transactionCountry, expectedCountry));
    }

    private static String requireCountry(String value)
    {
        var country = Objects.requireNonNull(value, "expectedCountry is required").strip().toUpperCase(Locale.ROOT);

        if (country.isEmpty())
        {
            throw new IllegalArgumentException("expectedCountry is required");
        }

        return country;
    }
}
