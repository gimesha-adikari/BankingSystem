package com.bankingsystem.core.features.transactions.idempotency.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity mapping for {@code core_transaction_idempotency} table introduced in V2.
 *
 * <p>Enforces transactional exactly-once command handling for core financial operations.
 * Arbitrary public setters are omitted to preserve integrity.
 */
@Entity
@Table(name = "core_transaction_idempotency", uniqueConstraints = {
        @UniqueConstraint(name = "uk_core_idem_user_op_key", columnNames = {"user_id", "operation_type", "client_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoreTransactionIdempotency {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", updatable = false, nullable = false, length = 30)
    private CoreOperationType operationType;

    @Column(name = "client_key", updatable = false, nullable = false, length = 128)
    private String clientKey;

    @Column(name = "request_hash", updatable = false, nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private CoreIdempotencyState state;

    @Column(name = "entry_id", columnDefinition = "BINARY(16)")
    private UUID entryId;

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public CoreTransactionIdempotency(UUID id, UUID userId, CoreOperationType operationType,
                                     String clientKey, String requestHash, CoreIdempotencyState state,
                                     LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.userId = Objects.requireNonNull(userId, "User ID must not be null");
        this.operationType = Objects.requireNonNull(operationType, "Operation type must not be null");
        this.clientKey = Objects.requireNonNull(clientKey, "Client key must not be null");
        this.requestHash = Objects.requireNonNull(requestHash, "Request hash must not be null");
        this.state = Objects.requireNonNull(state, "State must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at must not be null");
    }

    /**
     * Marks the claim as COMPLETED with the authoritative journal entry ID and cached HTTP response.
     */
    public void markCompleted(UUID entryId, int responseStatusCode, String responsePayload, LocalDateTime completedAt) {
        if (this.state == CoreIdempotencyState.COMPLETED) {
            throw new IllegalStateException("Idempotency claim is already in COMPLETED state");
        }
        this.entryId = Objects.requireNonNull(entryId, "Journal entry ID must not be null");
        this.responseStatusCode = responseStatusCode;
        this.responsePayload = responsePayload;
        this.completedAt = Objects.requireNonNull(completedAt, "Completed at must not be null");
        this.state = CoreIdempotencyState.COMPLETED;
    }
}
