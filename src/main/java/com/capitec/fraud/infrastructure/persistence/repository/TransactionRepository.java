package com.capitec.fraud.infrastructure.persistence.repository;

import com.capitec.fraud.infrastructure.persistence.entity.TransactionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>
{

    Optional<TransactionEntity> findByEventId(String eventId);

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    List<TransactionEntity> findByCustomerIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String customerId,
            Instant createdFrom,
            Instant createdTo);

    List<TransactionEntity> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String accountId,
            Instant createdFrom,
            Instant createdTo);
}
