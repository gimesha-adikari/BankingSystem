package com.bankingsystem.core.features.ledger.domain;

/**
 * Account classes matching V2 schema constraint:
 * {@code chk_ledger_accounts_class CHECK (account_class IN ('ASSET', 'LIABILITY', 'EQUITY'))}.
 */
public enum LedgerAccountClass {
    ASSET,
    LIABILITY,
    EQUITY
}
