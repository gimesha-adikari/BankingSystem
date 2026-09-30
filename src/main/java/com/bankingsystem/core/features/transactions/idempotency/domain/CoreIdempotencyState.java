package com.bankingsystem.core.features.transactions.idempotency.domain;

/**
 * Permitted lifecycle states for core transaction idempotency claims,
 * matching {@code chk_core_idem_state} in V2__core_ledger.sql.
 */
public enum CoreIdempotencyState {
    PROCESSING,
    COMPLETED
}
