package com.bankingsystem.core.features.ledger.domain;

/**
 * Journal posting direction matching V2 schema constraint:
 * {@code chk_journal_postings_direction CHECK (direction IN ('DEBIT', 'CREDIT'))}.
 */
public enum PostingDirection {
    DEBIT,
    CREDIT
}
