package com.bankingsystem.core.features.transactions.idempotency.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable outcome of an idempotent financial command execution.
 *
 * @param journalEntryId    Authoritative journal entry UUID from the ledger
 * @param httpStatusCode    Cached HTTP response status code (e.g. 200, 201)
 * @param responsePayload   Cached HTTP response payload string
 * @param replayed          True if result was returned from idempotent cache; false if newly executed
 */
public record IdempotentCommandResult(
        UUID journalEntryId,
        int httpStatusCode,
        String responsePayload,
        boolean replayed
) {

    public static final int MAX_PAYLOAD_BYTES = 61440; // 60 KiB defensive limit

    public IdempotentCommandResult {
        Objects.requireNonNull(journalEntryId, "Journal entry ID must not be null");
        if (httpStatusCode < 100 || httpStatusCode > 599) {
            throw new IllegalArgumentException("HTTP status code must be between 100 and 599, got: " + httpStatusCode);
        }
        if (responsePayload != null && responsePayload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Response payload exceeds maximum allowed size of " + MAX_PAYLOAD_BYTES + " bytes");
        }
    }

    public static IdempotentCommandResult newlyExecuted(UUID journalEntryId, int httpStatusCode, String responsePayload) {
        return new IdempotentCommandResult(journalEntryId, httpStatusCode, responsePayload, false);
    }

    public static IdempotentCommandResult replayed(UUID journalEntryId, int httpStatusCode, String responsePayload) {
        return new IdempotentCommandResult(journalEntryId, httpStatusCode, responsePayload, true);
    }
}
