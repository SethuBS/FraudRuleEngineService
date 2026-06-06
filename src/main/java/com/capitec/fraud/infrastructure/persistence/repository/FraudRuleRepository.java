package com.capitec.fraud.infrastructure.persistence.repository;

import com.capitec.fraud.infrastructure.persistence.entity.FraudRuleEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudRuleRepository extends JpaRepository<FraudRuleEntity, Long>
{

    Optional<FraudRuleEntity> findByCode(String code);

    List<FraudRuleEntity> findByEnabledTrueOrderByCodeAsc();

    List<FraudRuleEntity> findByUpdatedAtAfterOrderByUpdatedAtAsc(Instant updatedAfter);
}
