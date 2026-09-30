package com.bankingsystem.core.features.ledger.domain;

/**
 * Ledger actor types matching V2 schema constraint:
 * {@code chk_journal_entries_actor_type CHECK (actor_type IN ('USER', 'SYSTEM'))}.
 */
public enum LedgerActorType {
    USER,
    SYSTEM
}
