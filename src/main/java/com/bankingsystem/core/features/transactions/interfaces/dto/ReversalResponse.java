package com.bankingsystem.core.features.transactions.interfaces.dto;

import com.bankingsystem.core.features.ledger.domain.JournalEntryType;
import com.bankingsystem.core.features.transactions.application.ReversalReceipt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** Stable transport representation of the immutable internal reversal receipt. */
public record ReversalResponse(
        JournalEntryType operationType,
        UUID reversalJournalEntryId,
        String journalReference,
        UUID reversalOfEntryId,
        BigDecimal amount,
        String currency,
        Map<UUID, BigDecimal> resultingCustomerBalances,
        LocalDateTime postedAt,
        boolean replayed
) {

    public static ReversalResponse from(ReversalReceipt receipt) {
        return new ReversalResponse(
                receipt.operationType(),
                receipt.reversalJournalEntryId(),
                receipt.journalReference(),
                receipt.reversalOfEntryId(),
                receipt.amount(),
                receipt.currency(),
                receipt.resultingCustomerBalances(),
                receipt.postedAt(),
                receipt.replayed());
    }
}
