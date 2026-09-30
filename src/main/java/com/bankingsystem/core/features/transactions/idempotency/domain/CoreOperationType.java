package com.bankingsystem.core.features.transactions.idempotency.domain;

/**
 * Permitted operation types for core financial transaction idempotency,
 * matching {@code chk_core_idem_op_type} in V2__core_ledger.sql.
 */
public enum CoreOperationType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
