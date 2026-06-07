package com.capitec.fraud.application;

import static java.util.Comparator.comparing;

import com.capitec.fraud.domain.RuleEvaluationResult;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.rules.FraudRule;
import com.capitec.fraud.rules.HistoricalAverageAmountLookup;
import com.capitec.fraud.rules.RecentTransactionLookup;
import com.capitec.fraud.rules.TransactionContext;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
class FraudRuleEngine implements TransactionEvaluationEngine
{

    private final List<FraudRule> fraudRules;
    private final FraudRuleEnablement fraudRuleEnablement;
    private final RecentTransactionLookup recentTransactionLookup;
    private final HistoricalAverageAmountLookup historicalAverageAmountLookup;
    private final FraudDecisionService fraudDecisionService;
    private final Clock clock;

    FraudRuleEngine(
            List<FraudRule> fraudRules,
            FraudRuleEnablement fraudRuleEnablement,
            RecentTransactionLookup recentTransactionLookup,
            HistoricalAverageAmountLookup historicalAverageAmountLookup,
            FraudDecisionService fraudDecisionService,
            Clock clock)
    {
        this.fraudRules = fraudRules.stream()
                .sorted(comparing(FraudRule::code))
                .toList();
        this.fraudRuleEnablement = fraudRuleEnablement;
        this.recentTransactionLookup = recentTransactionLookup;
        this.historicalAverageAmountLookup = historicalAverageAmountLookup;
        this.fraudDecisionService = fraudDecisionService;
        this.clock = clock;
    }

    @Override
    public TransactionEvaluation evaluate(Transaction transaction)
    {
        var evaluatedAt = Instant.now(clock);
        var context = TransactionContext.current(
                transaction,
                recentTransactionLookup,
                historicalAverageAmountLookup);
        var ruleResults = fraudRules.stream()
                .filter(rule -> fraudRuleEnablement.isEnabled(rule.code()))
                .map(rule -> evaluateRule(rule, context, evaluatedAt))
                .toList();

        return fraudDecisionService.evaluate(transaction, ruleResults, evaluatedAt);
    }

    private RuleEvaluationResult evaluateRule(
            FraudRule rule,
            TransactionContext context,
            Instant evaluatedAt)
    {
        var match = rule.evaluate(context);

        return new RuleEvaluationResult(
                rule.code(),
                rule.name(),
                match.matched(),
                rule.defaultScore(),
                match.explanation(),
                evaluatedAt);
    }
}
