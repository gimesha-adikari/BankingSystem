package com.bankingsystem.core.features.transactions.idempotency.domain;

import com.bankingsystem.core.modules.common.exceptions.BusinessException;

/**
 * Thrown when an idempotency key is reused with a different semantic request payload.
 * Mapped to HTTP 409 Conflict in public API layers.
 */
public class IdempotencyConflictException extends BusinessException {

    public IdempotencyConflictException(String message) {
        super("ERR_IDEMPOTENCY_CONFLICT", message);
    }
}
