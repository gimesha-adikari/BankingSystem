package com.bankingsystem.core.features.ledger.domain;

/**
 * Journal entry statuses matching V2 schema constraint:
 * {@code chk_journal_entries_status CHECK (status = 'POSTED')}.
 */
public enum JournalEntryStatus {
    POSTED
}
