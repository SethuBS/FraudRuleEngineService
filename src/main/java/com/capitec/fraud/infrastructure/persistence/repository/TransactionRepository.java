package com.capitec.fraud.infrastructure.persistence.repository;

import com.capitec.fraud.infrastructure.persistence.entity.TransactionEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT transactionEntity
            FROM TransactionEntity transactionEntity
            WHERE (transactionEntity.customerId = :customerId OR transactionEntity.accountId = :accountId)
              AND transactionEntity.transactionTimestamp >= :windowStart
              AND transactionEntity.transactionTimestamp <= :windowEnd
              AND transactionEntity.transactionId <> :currentTransactionId
            ORDER BY transactionEntity.transactionTimestamp DESC
            """)
    List<TransactionEntity> findRecentByCustomerOrAccount(
            @Param("customerId") String customerId,
            @Param("accountId") String accountId,
            @Param("currentTransactionId") String currentTransactionId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);

    @Query(
            value = """
                    SELECT AVG(amount)
                    FROM transactions
                    WHERE (customer_id = :customerId OR account_id = :accountId)
                      AND transaction_id <> :currentTransactionId
                      AND currency = :currency
                    """,
            nativeQuery = true)
    Optional<BigDecimal> averageAmountByCustomerOrAccount(
            @Param("customerId") String customerId,
            @Param("accountId") String accountId,
            @Param("currentTransactionId") String currentTransactionId,
            @Param("currency") String currency);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE TransactionEntity transactionEntity
            SET transactionEntity.sanitizedRawPayload = null,
                transactionEntity.rawPayloadExpiresAt = null,
                transactionEntity.updatedAt = :cleanedAt
            WHERE transactionEntity.rawPayloadExpiresAt < :expiresBefore
              AND transactionEntity.sanitizedRawPayload IS NOT NULL
            """)
    int clearExpiredRawPayloads(
            @Param("expiresBefore") Instant expiresBefore,
            @Param("cleanedAt") Instant cleanedAt);
}
