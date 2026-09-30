package com.bankingsystem.core.features.ledger.domain;

/**
 * Account statuses matching V2 schema constraint:
 * {@code chk_ledger_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))}.
 */
public enum LedgerAccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
