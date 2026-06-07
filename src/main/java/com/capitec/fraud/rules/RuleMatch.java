package com.capitec.fraud.rules;

import java.util.Objects;

public record RuleMatch(boolean matched, String explanation)
{

    public RuleMatch
    {
        Objects.requireNonNull(explanation, "explanation is required");
        explanation = explanation.strip();

        if (explanation.isEmpty())
        {
            throw new IllegalArgumentException("explanation is required");
        }
    }

    public static RuleMatch matched(String explanation)
    {
        return new RuleMatch(true, explanation);
    }

    public static RuleMatch notMatched(String explanation)
    {
        return new RuleMatch(false, explanation);
    }
}
