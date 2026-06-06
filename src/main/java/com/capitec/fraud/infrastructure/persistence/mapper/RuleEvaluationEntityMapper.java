package com.capitec.fraud.infrastructure.persistence.mapper;

import com.capitec.fraud.domain.RiskScore;
import com.capitec.fraud.domain.RuleEvaluationResult;
import com.capitec.fraud.infrastructure.persistence.entity.FraudRuleEntity;
import com.capitec.fraud.infrastructure.persistence.entity.RuleEvaluationEntity;
import com.capitec.fraud.infrastructure.persistence.entity.TransactionEntity;

import org.springframework.stereotype.Component;

@Component
public class RuleEvaluationEntityMapper
{

    public RuleEvaluationEntity toEntity(
            RuleEvaluationResult result,
            TransactionEntity transaction)
    {
        return toEntity(result, transaction, null);
    }

    public RuleEvaluationEntity toEntity(
            RuleEvaluationResult result,
            TransactionEntity transaction,
            FraudRuleEntity fraudRule)
    {
        return new RuleEvaluationEntity(
                transaction,
                fraudRule,
                result.ruleCode(),
                result.ruleName(),
                result.matched(),
                result.scoreContribution().value(),
                result.explanation(),
                result.evaluatedAt());
    }

    public RuleEvaluationResult toDomain(RuleEvaluationEntity entity)
    {
        return new RuleEvaluationResult(
                entity.getRuleCode(),
                entity.getRuleName(),
                entity.isMatched(),
                RiskScore.of(entity.getScoreContribution()),
                entity.getExplanation(),
                entity.getEvaluatedAt());
    }
}
