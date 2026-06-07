package com.capitec.fraud.infrastructure.persistence.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fraud_rules")
public class FraudRuleEntity extends AuditedEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = SchemaColumns.RULE_CODE_LENGTH)
    private String code;

    @Column(name = "name", nullable = false, length = SchemaColumns.RULE_NAME_LENGTH)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = SchemaColumns.TEXT_COLUMN)
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "severity", nullable = false, length = SchemaColumns.SEVERITY_LENGTH)
    private String severity;

    @Column(name = "score", nullable = false)
    private int score;

    protected FraudRuleEntity()
    {
    }

    public FraudRuleEntity(
            String code,
            String name,
            String description,
            boolean enabled,
            String severity,
            int score)
    {
        this.code = code;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.severity = severity;
        this.score = score;
    }

    public Long getId()
    {
        return id;
    }

    public String getCode()
    {
        return code;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean metadataMatches(
            String nextName,
            String nextDescription,
            String nextSeverity,
            int nextScore)
    {
        return Objects.equals(name, nextName)
                && Objects.equals(description, nextDescription)
                && Objects.equals(severity, nextSeverity)
                && score == nextScore;
    }

    public void updateMetadata(
            String nextName,
            String nextDescription,
            String nextSeverity,
            int nextScore)
    {
        name = nextName;
        description = nextDescription;
        severity = nextSeverity;
        score = nextScore;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public String getSeverity()
    {
        return severity;
    }

    public int getScore()
    {
        return score;
    }
}
