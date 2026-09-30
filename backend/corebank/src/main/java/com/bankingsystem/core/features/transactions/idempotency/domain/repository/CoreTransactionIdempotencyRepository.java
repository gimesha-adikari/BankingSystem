package com.bankingsystem.core.features.transactions.idempotency.domain.repository;

import com.bankingsystem.core.features.transactions.idempotency.domain.CoreIdempotencyState;
import com.bankingsystem.core.features.transactions.idempotency.domain.CoreOperationType;
import com.bankingsystem.core.features.transactions.idempotency.domain.CoreTransactionIdempotency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link CoreTransactionIdempotency}.
 *
 * <p>Production orchestration NEVER deletes completed idempotency records; no routine
 * delete methods are exposed or utilized by application services.
 */
@Repository
public interface CoreTransactionIdempotencyRepository extends JpaRepository<CoreTransactionIdempotency, UUID> {

    /**
     * Atomically attempts to claim idempotency for {@code (user_id, operation_type, client_key)}
     * using MySQL's {@code INSERT IGNORE}.
     *
     * <p>Returns:
     * <ul>
     *   <li>{@code 1} if this transaction successfully inserted the claim row and owns execution.</li>
     *   <li>{@code 0} if a claim already exists or a concurrent transaction won the claim.</li>
     * </ul>
     * This avoids throwing a {@code DataIntegrityViolationException} which would poison the
     * current database transaction.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO core_transaction_idempotency " +
            "(id, user_id, operation_type, client_key, request_hash, state, created_at) " +
            "VALUES (UUID_TO_BIN(:id), UUID_TO_BIN(:userId), :operationType, :clientKey, :requestHash, :state, :createdAt)",
            nativeQuery = true)
    int insertIgnoreClaim(
            @Param("id") String id,
            @Param("userId") String userId,
            @Param("operationType") String operationType,
            @Param("clientKey") String clientKey,
            @Param("requestHash") String requestHash,
            @Param("state") String state,
            @Param("createdAt") LocalDateTime createdAt
    );

    /**
     * Current / locking read on the idempotency claim using {@code PESSIMISTIC_WRITE} ({@code FOR UPDATE}).
     * Waits for any concurrent transaction holding the row lock to commit or rollback.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CoreTransactionIdempotency c WHERE c.userId = :userId AND c.operationType = :operationType AND c.clientKey = :clientKey")
    Optional<CoreTransactionIdempotency> findByUserIdAndOperationTypeAndClientKeyForUpdate(
            @Param("userId") UUID userId,
            @Param("operationType") CoreOperationType operationType,
            @Param("clientKey") String clientKey
    );

    /**
     * Read-only lookup by user, operation, and client key.
     */
    Optional<CoreTransactionIdempotency> findByUserIdAndOperationTypeAndClientKey(
            UUID userId,
            CoreOperationType operationType,
            String clientKey
    );

    /**
     * Lookup by authoritative journal entry ID.
     */
    Optional<CoreTransactionIdempotency> findByEntryId(UUID entryId);
}
