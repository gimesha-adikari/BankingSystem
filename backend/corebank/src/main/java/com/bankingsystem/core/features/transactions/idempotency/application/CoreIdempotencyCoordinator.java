package com.bankingsystem.core.features.transactions.idempotency.application;

import com.bankingsystem.core.features.transactions.idempotency.domain.*;
import com.bankingsystem.core.features.transactions.idempotency.domain.repository.CoreTransactionIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Transactional coordinator for core financial command idempotency.
 *
 * <p>Enforces exactly-once execution semantics by coordinating the database claim row
 * and the financial command (PostingEngine) within the SAME physical database transaction.
 *
 * <p>This coordinator sits as an orchestration layer above PostingEngine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoreIdempotencyCoordinator {

    private static final Pattern HEX_64 = Pattern.compile("^[0-9a-f]{64}$");

    private final CoreTransactionIdempotencyRepository idempotencyRepository;

    /**
     * Executes a financial command within an atomic idempotency boundary.
     *
     * @param userId        Authenticated customer UUID
     * @param operationType Operation category (DEPOSIT, WITHDRAWAL, TRANSFER)
     * @param clientKey     Validated client-supplied idempotency key
     * @param requestHash   Canonical 64-char lowercase hexadecimal SHA-256 fingerprint
     * @param action        Financial command to execute if claim is obtained
     * @return IdempotentCommandResult representing newly executed or replayed result
     */
    @Transactional
    public IdempotentCommandResult execute(
            UUID userId,
            CoreOperationType operationType,
            IdempotencyKey clientKey,
            String requestHash,
            IdempotentAction action
    ) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(operationType, "Operation type must not be null");
        Objects.requireNonNull(clientKey, "Client key must not be null");
        Objects.requireNonNull(requestHash, "Request hash must not be null");
        Objects.requireNonNull(action, "Financial action must not be null");

        if (!HEX_64.matcher(requestHash).matches()) {
            throw new IllegalArgumentException("Request hash must be a 64-character lowercase hexadecimal string");
        }

        UUID claimId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // 1. Attempt atomic claim insert via INSERT IGNORE to avoid poisoning the transaction on duplicate keys
        int rowsInserted = idempotencyRepository.insertIgnoreClaim(
                claimId.toString(),
                userId.toString(),
                operationType.name(),
                clientKey.getValue(),
                requestHash,
                CoreIdempotencyState.PROCESSING.name(),
                now
        );

        if (rowsInserted == 1) {
            // This transaction won the claim and is the authoritative owner
            return executeAsOwner(claimId, action);
        }

        // 2. Claim insert returned 0: an existing or concurrent claim holds the key.
        // Perform a current locking read (SELECT ... FOR UPDATE) to wait for concurrent owner completion or rollback.
        return handleExistingClaim(userId, operationType, clientKey, requestHash, action);
    }

    private IdempotentCommandResult executeAsOwner(UUID claimId, IdempotentAction action) {
        CoreTransactionIdempotency claim = idempotencyRepository.findById(claimId)
                .orElseThrow(() -> new IllegalStateException("Idempotency claim record missing after insert"));

        if (claim.getState() != CoreIdempotencyState.PROCESSING) {
            throw new IllegalStateException("Newly inserted claim is not in PROCESSING state");
        }

        // Execute financial work exactly once
        IdempotentCommandResult result = action.execute();
        if (result == null) {
            throw new IllegalStateException("Financial action returned null result");
        }
        if (result.journalEntryId() == null) {
            throw new IllegalStateException("Financial action must produce an authoritative journal entry ID");
        }

        // Transition claim to COMPLETED in the same transaction
        claim.markCompleted(
                result.journalEntryId(),
                result.httpStatusCode(),
                result.responsePayload(),
                LocalDateTime.now()
        );
        idempotencyRepository.saveAndFlush(claim);

        return IdempotentCommandResult.newlyExecuted(
                result.journalEntryId(),
                result.httpStatusCode(),
                result.responsePayload()
        );
    }

    private IdempotentCommandResult handleExistingClaim(
            UUID userId,
            CoreOperationType operationType,
            IdempotencyKey clientKey,
            String requestHash,
            IdempotentAction action
    ) {
        Optional<CoreTransactionIdempotency> existingOpt = idempotencyRepository
                .findByUserIdAndOperationTypeAndClientKeyForUpdate(userId, operationType, clientKey.getValue());

        if (existingOpt.isEmpty()) {
            // Concurrent owner rolled back while this transaction was waiting on row lock!
            // Re-attempt claim insertion now that the competing transaction has aborted.
            UUID retryClaimId = UUID.randomUUID();
            int retryInsert = idempotencyRepository.insertIgnoreClaim(
                    retryClaimId.toString(),
                    userId.toString(),
                    operationType.name(),
                    clientKey.getValue(),
                    requestHash,
                    CoreIdempotencyState.PROCESSING.name(),
                    LocalDateTime.now()
            );
            if (retryInsert == 1) {
                return executeAsOwner(retryClaimId, action);
            }
            existingOpt = idempotencyRepository
                    .findByUserIdAndOperationTypeAndClientKeyForUpdate(userId, operationType, clientKey.getValue());
        }

        CoreTransactionIdempotency existing = existingOpt
                .orElseThrow(() -> new IllegalStateException("Unable to resolve idempotency claim for key: " + clientKey));

        // 1. Verify semantic request fingerprint matches stored request hash
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Incoming request hash does not match stored request hash for key: " + clientKey.getValue()
            );
        }

        // 2. Inspect state
        if (existing.getState() == CoreIdempotencyState.COMPLETED) {
            log.info("Replaying cached idempotency result for user={}, op={}, key={}, entryId={}",
                    userId, operationType, clientKey.getValue(), existing.getEntryId());
            return IdempotentCommandResult.replayed(
                    existing.getEntryId(),
                    existing.getResponseStatusCode(),
                    existing.getResponsePayload()
            );
        }

        if (existing.getState() == CoreIdempotencyState.PROCESSING) {
            throw new IdempotencyInProgressException(
                    "Idempotency claim is currently in progress or stalled for key: " + clientKey.getValue()
            );
        }

        throw new IllegalStateException("Unexpected idempotency state: " + existing.getState());
    }
}
