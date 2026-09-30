package com.bankingsystem.core.features.transactions.idempotency.domain;

import com.bankingsystem.core.modules.common.exceptions.BusinessException;

/**
 * Thrown when an existing idempotency record is locked and observed in PROCESSING state.
 * Indicates an in-flight operation or an operational anomaly requiring investigation.
 */
public class IdempotencyInProgressException extends BusinessException {

    public IdempotencyInProgressException(String message) {
        super("ERR_IDEMPOTENCY_IN_PROGRESS", message);
    }
}
