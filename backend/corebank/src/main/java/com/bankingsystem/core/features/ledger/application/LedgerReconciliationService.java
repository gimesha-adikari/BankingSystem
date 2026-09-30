package com.bankingsystem.core.features.ledger.application;

import java.util.UUID;

/**
 * Internal read-only service that detects drift between materialized {@code accounts.balance}
 * and derived double-entry liability ledger postings.
 *
 * <p>Read-only: detects discrepancies, never overwrites or repairs.
 */
public interface LedgerReconciliationService {

    /**
     * Reconcile all customer accounts in the database against their ledger postings.
     */
    LedgerReconciliationResult reconcileAll();

    /**
     * Reconcile a single customer account against its ledger postings.
     */
    LedgerReconciliationResult reconcileAccount(UUID customerAccountId);
}
