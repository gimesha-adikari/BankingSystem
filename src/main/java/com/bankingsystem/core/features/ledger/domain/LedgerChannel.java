package com.bankingsystem.core.features.ledger.domain;

/**
 * Ledger channel matching V2 schema constraint:
 * {@code chk_journal_entries_channel CHECK (channel IN ('WEB', 'MOBILE', 'TELLER', 'SYSTEM'))}.
 */
public enum LedgerChannel {
    WEB,
    MOBILE,
    TELLER,
    SYSTEM
}
