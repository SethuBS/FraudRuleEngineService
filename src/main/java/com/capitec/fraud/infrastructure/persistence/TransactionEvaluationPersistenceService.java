package com.capitec.fraud.infrastructure.persistence;

import com.capitec.fraud.domain.FraudAlert;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.persistence.entity.FraudAlertEntity;
import com.capitec.fraud.infrastructure.persistence.entity.RuleEvaluationEntity;
import com.capitec.fraud.infrastructure.persistence.entity.TransactionEntity;
import com.capitec.fraud.infrastructure.persistence.mapper.FraudAlertEntityMapper;
import com.capitec.fraud.infrastructure.persistence.mapper.RuleEvaluationEntityMapper;
import com.capitec.fraud.infrastructure.persistence.mapper.TransactionEntityMapper;
import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;
import com.capitec.fraud.infrastructure.persistence.repository.FraudRuleRepository;
import com.capitec.fraud.infrastructure.persistence.repository.RuleEvaluationRepository;
import com.capitec.fraud.infrastructure.persistence.repository.TransactionRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionEvaluationPersistenceService
{

    private final TransactionRepository transactionRepository;
    private final RuleEvaluationRepository ruleEvaluationRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionEntityMapper transactionEntityMapper;
    private final RuleEvaluationEntityMapper ruleEvaluationEntityMapper;
    private final FraudAlertEntityMapper fraudAlertEntityMapper;

    public TransactionEvaluationPersistenceService(
            TransactionRepository transactionRepository,
            RuleEvaluationRepository ruleEvaluationRepository,
            FraudRuleRepository fraudRuleRepository,
            FraudAlertRepository fraudAlertRepository,
            TransactionEntityMapper transactionEntityMapper,
            RuleEvaluationEntityMapper ruleEvaluationEntityMapper,
            FraudAlertEntityMapper fraudAlertEntityMapper)
    {
        this.transactionRepository = transactionRepository;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionEntityMapper = transactionEntityMapper;
        this.ruleEvaluationEntityMapper = ruleEvaluationEntityMapper;
        this.fraudAlertEntityMapper = fraudAlertEntityMapper;
    }

    @Transactional
    public TransactionEntity persistEvaluation(TransactionEvaluation evaluation)
    {
        var transaction = getOrCreateTransaction(evaluation);
        var ruleEvaluations = evaluation.ruleResults()
                .stream()
                .map(result -> ruleEvaluationEntityMapper.toEntity(
                        result,
                        transaction,
                        fraudRuleRepository.findByCode(result.ruleCode()).orElse(null)))
                .toList();

        ruleEvaluationRepository.saveAll(ruleEvaluations);

        return transaction;
    }

    @Transactional
    public FraudAlertEntity persistAlert(FraudAlert alert)
    {
        var transaction = transactionRepository.findByTransactionId(alert.transaction().transactionId())
                .orElseGet(() -> transactionRepository.save(transactionEntityMapper.toEntity(alert.transaction())));

        return fraudAlertRepository.save(fraudAlertEntityMapper.toEntity(alert, transaction));
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

    private TransactionEntity getOrCreateTransaction(TransactionEvaluation evaluation)
    {
        return transactionRepository.findByTransactionId(evaluation.transaction().transactionId())
                .orElseGet(() -> transactionRepository.save(transactionEntityMapper.toEntity(evaluation.transaction())));
    }
}
