package com.capitec.fraud.infrastructure.persistence.repository;

import com.capitec.fraud.domain.RiskLevel;
import com.capitec.fraud.infrastructure.persistence.entity.FraudAlertEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudAlertRepository extends JpaRepository<FraudAlertEntity, Long>
{

    Optional<FraudAlertEntity> findByAlertId(UUID alertId);

    Optional<FraudAlertEntity> findFirstByTransactionTransactionIdOrderByCreatedAtAsc(String transactionId);

    List<FraudAlertEntity> findByTransactionTransactionIdOrderByCreatedAtDesc(String transactionId);

    List<FraudAlertEntity> findByCustomerIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String customerId,
            Instant createdFrom,
            Instant createdTo);

    List<FraudAlertEntity> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String accountId,
            Instant createdFrom,
            Instant createdTo);

    List<FraudAlertEntity> findByRiskLevelAndCreatedAtBetweenOrderByCreatedAtDesc(
            RiskLevel riskLevel,
            Instant createdFrom,
            Instant createdTo);

    List<FraudAlertEntity> findByCustomerIdAndAccountIdAndRiskLevelAndCreatedAtBetweenOrderByCreatedAtDesc(
            String customerId,
            String accountId,
            RiskLevel riskLevel,
            Instant createdFrom,
            Instant createdTo);
}
