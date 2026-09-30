package com.bankingsystem.core.features.transactions.application;

import com.bankingsystem.core.features.accounts.domain.Account;
import com.bankingsystem.core.features.ledger.domain.JournalEntry;
import com.bankingsystem.core.features.transactions.domain.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Internal synchronous projection service that projects successful PostingEngine
 * operations into the legacy transactions table for compatibility with existing read clients.
 *
 * This is a READ MODEL / COMPATIBILITY VIEW. It is not the accounting source of truth.
 */
public interface LegacyTransactionProjectionService {

    /**
     * Projects a posted journal entry into one or more legacy Transaction rows.
     *
     * @param journalEntry the authoritative persisted journal entry
     * @param customerAccounts the locked customer Account entities with final balances
     * @param netDeltas the net balance delta for each affected customer account
     * @return list of persisted Transaction projection entities
     */
    List<Transaction> projectTransactions(
            JournalEntry journalEntry,
            Map<UUID, Account> customerAccounts,
            Map<UUID, BigDecimal> netDeltas
    );
}
