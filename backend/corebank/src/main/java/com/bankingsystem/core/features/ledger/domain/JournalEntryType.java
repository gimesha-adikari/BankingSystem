package com.bankingsystem.core.features.ledger.domain;

/**
 * Journal entry types matching V2 schema constraint:
 * {@code chk_journal_entries_type CHECK (entry_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'REVERSAL', 'OPENING_BALANCE'))}.
 */
public enum JournalEntryType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    REVERSAL,
    OPENING_BALANCE
}
