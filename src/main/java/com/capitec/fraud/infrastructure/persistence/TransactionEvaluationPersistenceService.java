package com.capitec.fraud.infrastructure.persistence;

import com.capitec.fraud.application.AlertIdGenerator;
import com.capitec.fraud.domain.FraudAlert;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.config.FraudIdempotencyProperties;
import com.capitec.fraud.infrastructure.persistence.entity.FraudAlertEntity;
import com.capitec.fraud.infrastructure.persistence.entity.ProcessedEventEntity;
import com.capitec.fraud.infrastructure.persistence.entity.RuleEvaluationEntity;
import com.capitec.fraud.infrastructure.persistence.entity.TransactionEntity;
import com.capitec.fraud.infrastructure.persistence.mapper.FraudAlertEntityMapper;
import com.capitec.fraud.infrastructure.persistence.mapper.RuleEvaluationEntityMapper;
import com.capitec.fraud.infrastructure.persistence.mapper.TransactionEntityMapper;
import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;
import com.capitec.fraud.infrastructure.persistence.repository.FraudRuleRepository;
import com.capitec.fraud.infrastructure.persistence.repository.ProcessedEventRepository;
import com.capitec.fraud.infrastructure.persistence.repository.RuleEvaluationRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionEvaluationPersistenceService
{

    private final TransactionRepository transactionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final RuleEvaluationRepository ruleEvaluationRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionEntityMapper transactionEntityMapper;
    private final RuleEvaluationEntityMapper ruleEvaluationEntityMapper;
    private final FraudAlertEntityMapper fraudAlertEntityMapper;
    private final FraudIdempotencyProperties idempotencyProperties;
    private final AlertIdGenerator alertIdGenerator;

    public TransactionEvaluationPersistenceService(
            TransactionRepository transactionRepository,
            ProcessedEventRepository processedEventRepository,
            RuleEvaluationRepository ruleEvaluationRepository,
            FraudRuleRepository fraudRuleRepository,
            FraudAlertRepository fraudAlertRepository,
            TransactionEntityMapper transactionEntityMapper,
            RuleEvaluationEntityMapper ruleEvaluationEntityMapper,
            FraudAlertEntityMapper fraudAlertEntityMapper,
            FraudIdempotencyProperties idempotencyProperties,
            AlertIdGenerator alertIdGenerator)
    {
        this.transactionRepository = transactionRepository;
        this.processedEventRepository = processedEventRepository;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionEntityMapper = transactionEntityMapper;
        this.ruleEvaluationEntityMapper = ruleEvaluationEntityMapper;
        this.fraudAlertEntityMapper = fraudAlertEntityMapper;
        this.idempotencyProperties = idempotencyProperties;
        this.alertIdGenerator = alertIdGenerator;
    }

    @Transactional
    public TransactionEvaluation persistProcessedEvaluation(TransactionEvaluation evaluation)
    {
        processedEventRepository.saveAndFlush(new ProcessedEventEntity(
                evaluation.transaction().eventId(),
                evaluation.transaction().transactionId(),
                idempotencyProperties.completedStatus(),
                evaluation.evaluatedAt(),
                null,
                null));

        var transaction = transactionRepository.saveAndFlush(transactionEntityMapper.toEntity(evaluation.transaction()));

        persistRuleEvaluations(evaluation, transaction);
        persistEvaluationAlert(evaluation);

        return evaluation;
    }

    @Transactional
    public TransactionEntity persistEvaluation(TransactionEvaluation evaluation)
    {
        var transaction = getOrCreateTransaction(evaluation);
        persistRuleEvaluations(evaluation, transaction);

        return transaction;
    }

    @Transactional
    public FraudAlertEntity persistAlert(FraudAlert alert)
    {
        return fraudAlertRepository.findFirstByTransactionTransactionIdOrderByCreatedAtAsc(alert.transaction().transactionId())
                .orElseGet(() -> saveAlert(alert));
    }

    @Transactional(readOnly = true)
    public Optional<TransactionEvaluation> findEvaluationByEventId(String eventId, RiskPolicy riskPolicy)
    {
        return processedEventRepository.findByEventId(eventId)
                .map(ProcessedEventEntity::getTransactionId)
                .flatMap(transactionId -> findEvaluationByTransactionId(transactionId, riskPolicy));
    }

    @Transactional(readOnly = true)
    public Optional<TransactionEvaluation> findEvaluationByTransactionId(String transactionId, RiskPolicy riskPolicy)
    {
        return transactionRepository.findByTransactionId(transactionId)
                .map(transaction -> toDomainEvaluation(transaction, riskPolicy));
    }

    @Transactional(readOnly = true)
    public Optional<TransactionEntity> findTransactionByTransactionId(String transactionId)
    {
        return transactionRepository.findByTransactionId(transactionId);
    }

    @Transactional(readOnly = true)
    public List<RuleEvaluationEntity> findRuleEvaluationsByTransactionId(String transactionId)
    {
        return ruleEvaluationRepository.findByTransactionTransactionIdOrderByCreatedAtAsc(transactionId);
    }

    private void persistRuleEvaluations(TransactionEvaluation evaluation, TransactionEntity transaction)
    {
        var ruleEvaluations = evaluation.ruleResults()
                .stream()
                .map(result -> ruleEvaluationEntityMapper.toEntity(
                        result,
                        transaction,
                        fraudRuleRepository.findByCode(result.ruleCode()).orElse(null)))
                .toList();

        ruleEvaluationRepository.saveAll(ruleEvaluations);
    }

    private void persistEvaluationAlert(TransactionEvaluation evaluation)
    {
        evaluation.toFraudAlert(alertIdGenerator.nextAlertId()).ifPresent(this::saveAlert);
    }

    private FraudAlertEntity saveAlert(FraudAlert alert)
    {
        var transaction = transactionRepository.findByTransactionId(alert.transaction().transactionId())
                .orElseGet(() -> transactionRepository.save(transactionEntityMapper.toEntity(alert.transaction())));

        return fraudAlertRepository.save(fraudAlertEntityMapper.toEntity(alert, transaction));
    }

    private TransactionEvaluation toDomainEvaluation(TransactionEntity transaction, RiskPolicy riskPolicy)
    {
        var transactionId = transaction.getTransactionId();
        var ruleResults = ruleEvaluationRepository.findByTransactionTransactionIdOrderByCreatedAtAsc(transactionId)
                .stream()
                .map(ruleEvaluationEntityMapper::toDomain)
                .toList();

        return TransactionEvaluation.from(
                transactionEntityMapper.toDomain(transaction),
                ruleResults,
                riskPolicy,
                resolvedEvaluationTime(transaction, ruleResults));
    }

    private Instant resolvedEvaluationTime(
            TransactionEntity transaction,
            List<com.capitec.fraud.domain.RuleEvaluationResult> ruleResults)
    {
        return processedEventRepository.findFirstByTransactionIdOrderByCreatedAtAsc(transaction.getTransactionId())
                .map(ProcessedEventEntity::getEvaluatedAt)
                .or(() -> ruleResults.stream().map(com.capitec.fraud.domain.RuleEvaluationResult::evaluatedAt).findFirst())
                .orElse(transaction.getCreatedAt());
    }

    private TransactionEntity getOrCreateTransaction(TransactionEvaluation evaluation)
    {
        return transactionRepository.findByTransactionId(evaluation.transaction().transactionId())
                .orElseGet(() -> transactionRepository.save(transactionEntityMapper.toEntity(evaluation.transaction())));
    }
}
