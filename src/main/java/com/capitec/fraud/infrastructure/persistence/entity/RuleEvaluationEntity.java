package com.capitec.fraud.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rule_evaluations")
public class RuleEvaluationEntity extends AuditedEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id", nullable = false)
    private TransactionEntity transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fraud_rule_id")
    private FraudRuleEntity fraudRule;

    @Column(name = "rule_code", nullable = false, length = SchemaColumns.RULE_CODE_LENGTH)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = SchemaColumns.RULE_NAME_LENGTH)
    private String ruleName;

    @Column(name = "matched", nullable = false)
    private boolean matched;

    @Column(name = "score_contribution", nullable = false)
    private int scoreContribution;

    @Column(name = "explanation", nullable = false, columnDefinition = SchemaColumns.TEXT_COLUMN)
    private String explanation;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected RuleEvaluationEntity()
    {
    }

    public RuleEvaluationEntity(
            TransactionEntity transaction,
            FraudRuleEntity fraudRule,
            String ruleCode,
            String ruleName,
            boolean matched,
            int scoreContribution,
            String explanation,
            Instant evaluatedAt)
    {
        this.transaction = transaction;
        this.fraudRule = fraudRule;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.matched = matched;
        this.scoreContribution = scoreContribution;
        this.explanation = explanation;
        this.evaluatedAt = evaluatedAt;
    }

    public Long getId()
    {
        return id;
    }

    public TransactionEntity getTransaction()
    {
        return transaction;
    }

    public FraudRuleEntity getFraudRule()
    {
        return fraudRule;
    }

    public String getRuleCode()
    {
        return ruleCode;
    }

    public String getRuleName()
    {
        return ruleName;
    }

    public boolean isMatched()
    {
        return matched;
    }

    public int getScoreContribution()
    {
        return scoreContribution;
    }

    public String getExplanation()
    {
        return explanation;
    }

    public Instant getEvaluatedAt()
    {
        return evaluatedAt;
    }
}
