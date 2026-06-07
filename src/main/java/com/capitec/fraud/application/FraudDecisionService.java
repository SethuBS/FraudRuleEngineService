package com.capitec.fraud.application;

import com.capitec.fraud.domain.FraudDecision;
import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.RuleEvaluationResult;
import com.capitec.fraud.domain.Transaction;
import com.capitec.fraud.domain.TransactionEvaluation;

import java.time.Instant;
import java.util.Collection;

import org.springframework.stereotype.Service;

@Service
class FraudDecisionService
{

    private final RiskPolicy riskPolicy;

    FraudDecisionService(RiskPolicy riskPolicy)
    {
        this.riskPolicy = riskPolicy;
    }

    TransactionEvaluation evaluate(
            Transaction transaction,
            Collection<RuleEvaluationResult> ruleResults,
            Instant evaluatedAt)
    {
        return TransactionEvaluation.from(transaction, ruleResults, riskPolicy, evaluatedAt);
    }

    RiskScore riskScore(Collection<RuleEvaluationResult> ruleResults)
    {
        return riskPolicy.aggregateScore(ruleResults);
    }

    RiskLevel riskLevel(RiskScore riskScore)
    {
        return riskPolicy.riskLevelFor(riskScore);
    }

    FraudDecision decision(RiskScore riskScore)
    {
        return riskPolicy.decisionFor(riskScore);
    }
}
