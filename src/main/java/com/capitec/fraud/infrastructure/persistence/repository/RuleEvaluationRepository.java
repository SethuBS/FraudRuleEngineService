package com.capitec.fraud.infrastructure.persistence.repository;

import com.capitec.fraud.infrastructure.persistence.entity.RuleEvaluationEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleEvaluationRepository extends JpaRepository<RuleEvaluationEntity, Long>
{

    List<RuleEvaluationEntity> findByTransactionTransactionIdOrderByCreatedAtAsc(String transactionId);

    List<RuleEvaluationEntity> findByTransactionTransactionIdAndMatchedOrderByCreatedAtAsc(
            String transactionId,
            boolean matched);
}
