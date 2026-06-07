package com.capitec.fraud.infrastructure.persistence;

import com.capitec.fraud.application.FraudAlertSearchQuery;
import com.capitec.fraud.application.FraudAlertView;
import com.capitec.fraud.application.FraudRetrievalService;
import com.capitec.fraud.application.PageResult;
import com.capitec.fraud.domain.RiskPolicy;
import com.capitec.fraud.domain.TransactionEvaluation;
import com.capitec.fraud.infrastructure.persistence.entity.FraudAlertEntity;
import com.capitec.fraud.infrastructure.persistence.repository.FraudAlertRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseFraudRetrievalService implements FraudRetrievalService
{

    private static final String ALERT_CREATED_AT_SORT_FIELD = "createdAt";
    private static final String ALERT_ID_SORT_FIELD = "id";
    private static final String ALERT_CUSTOMER_ID_FIELD = "customerId";
    private static final String ALERT_ACCOUNT_ID_FIELD = "accountId";
    private static final String ALERT_RISK_LEVEL_FIELD = "riskLevel";

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionEvaluationPersistenceService transactionEvaluationPersistenceService;
    private final RiskPolicy riskPolicy;

    public DatabaseFraudRetrievalService(
            FraudAlertRepository fraudAlertRepository,
            TransactionEvaluationPersistenceService transactionEvaluationPersistenceService,
            RiskPolicy riskPolicy)
    {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionEvaluationPersistenceService = transactionEvaluationPersistenceService;
        this.riskPolicy = riskPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FraudAlertView> findAlerts(FraudAlertSearchQuery query)
    {
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(
                        Sort.Order.desc(ALERT_CREATED_AT_SORT_FIELD),
                        Sort.Order.desc(ALERT_ID_SORT_FIELD)));
        var alerts = fraudAlertRepository.findAll(toSpecification(query), pageable);

        return new PageResult<>(
                alerts.getContent().stream().map(this::toView).toList(),
                alerts.getNumber(),
                alerts.getSize(),
                alerts.getTotalElements(),
                alerts.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FraudAlertView> findAlert(UUID alertId)
    {
        return fraudAlertRepository.findByAlertId(alertId).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionEvaluation> findEvaluation(String transactionId)
    {
        return transactionEvaluationPersistenceService.findEvaluationByTransactionId(transactionId, riskPolicy);
    }

    private FraudAlertView toView(FraudAlertEntity alert)
    {
        return new FraudAlertView(
                alert.getAlertId(),
                alert.getTransaction().getTransactionId(),
                alert.getCustomerId(),
                alert.getAccountId(),
                alert.getDecision(),
                alert.getRiskScore(),
                alert.getRiskLevel(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                alert.getClosedAt());
    }

    private static Specification<FraudAlertEntity> toSpecification(FraudAlertSearchQuery query)
    {
        return equalIfPresent(ALERT_CUSTOMER_ID_FIELD, query.customerId())
                .and(equalIfPresent(ALERT_ACCOUNT_ID_FIELD, query.accountId()))
                .and(equalIfPresent(ALERT_RISK_LEVEL_FIELD, query.riskLevel()))
                .and(createdAtFrom(query.fromDate()))
                .and(createdAtTo(query.toDate()));
    }

    private static Specification<FraudAlertEntity> equalIfPresent(String field, Object value)
    {
        return (root, criteriaQuery, criteriaBuilder) ->
        {
            if (value == null)
            {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get(field), value);
        };
    }

    private static Specification<FraudAlertEntity> createdAtFrom(Instant fromDate)
    {
        return (root, criteriaQuery, criteriaBuilder) ->
        {
            if (fromDate == null)
            {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(root.<Instant>get(ALERT_CREATED_AT_SORT_FIELD), fromDate);
        };
    }

    private static Specification<FraudAlertEntity> createdAtTo(Instant toDate)
    {
        return (root, criteriaQuery, criteriaBuilder) ->
        {
            if (toDate == null)
            {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThanOrEqualTo(root.<Instant>get(ALERT_CREATED_AT_SORT_FIELD), toDate);
        };
    }
}
