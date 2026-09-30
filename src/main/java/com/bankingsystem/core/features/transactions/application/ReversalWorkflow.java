package com.bankingsystem.core.features.transactions.application;

import java.util.UUID;

/** Internal full-reversal workflow. HTTP transport is intentionally separate. */
public interface ReversalWorkflow {

    ReversalReceipt reverse(UUID trustedActorUserId, UUID originalJournalEntryId, String reason);
}
