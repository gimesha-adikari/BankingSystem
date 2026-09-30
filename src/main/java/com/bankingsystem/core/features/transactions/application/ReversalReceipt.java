package com.bankingsystem.core.features.transactions.application;

import com.bankingsystem.core.features.ledger.domain.JournalEntryType;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable internal receipt for a complete journal reversal. */
public record ReversalReceipt(
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

    public ReversalReceipt {
        if (operationType != JournalEntryType.REVERSAL) {
            throw new IllegalArgumentException("Reversal receipt must have REVERSAL operation type");
        }
        Objects.requireNonNull(reversalJournalEntryId, "Reversal journal entry ID must not be null");
        Objects.requireNonNull(journalReference, "Journal reference must not be null");
        Objects.requireNonNull(reversalOfEntryId, "Reversed original entry ID must not be null");
        amount = normalize(amount, "Amount");
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be null or blank");
        }
        Objects.requireNonNull(resultingCustomerBalances, "Resulting balances must not be null");
        resultingCustomerBalances = Collections.unmodifiableMap(Map.copyOf(resultingCustomerBalances));
        Objects.requireNonNull(postedAt, "Posted timestamp must not be null");
    }

    private static BigDecimal normalize(BigDecimal value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        return value.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);
    }
}
