package com.capitec.fraud.infrastructure.persistence.repository;

import com.capitec.fraud.infrastructure.persistence.entity.ProcessedEventEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long>
{

    Optional<ProcessedEventEntity> findByEventId(String eventId);

    Optional<ProcessedEventEntity> findFirstByTransactionIdOrderByCreatedAtAsc(String transactionId);

    boolean existsByEventId(String eventId);
}
