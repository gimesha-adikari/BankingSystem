package com.bankingsystem.core.features.transactions.application;

import com.bankingsystem.core.features.ledger.domain.JournalEntryType;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Immutable, JSON-serializable receipt for a customer-to-customer transfer. */
public record TransferReceipt(
        JournalEntryType operationType,
        UUID journalEntryId,
        String journalReference,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        BigDecimal sourceResultingBalance,
        BigDecimal destinationResultingBalance,
        LocalDateTime postedAt,
        boolean replayed
) {

    public TransferReceipt {
        if (operationType != JournalEntryType.TRANSFER) {
            throw new IllegalArgumentException("Transfer receipt must have TRANSFER operation type");
        }
        Objects.requireNonNull(journalEntryId, "Journal entry ID must not be null");
        Objects.requireNonNull(journalReference, "Journal reference must not be null");
        Objects.requireNonNull(sourceAccountId, "Source account ID must not be null");
        Objects.requireNonNull(destinationAccountId, "Destination account ID must not be null");
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination account IDs must differ");
        }
        amount = normalize(amount, "Amount");
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be null or blank");
        }
        sourceResultingBalance = normalize(sourceResultingBalance, "Source resulting balance");
        destinationResultingBalance = normalize(destinationResultingBalance, "Destination resulting balance");
        Objects.requireNonNull(postedAt, "Posted timestamp must not be null");
    }

    public TransferReceipt withReplayed(boolean value) {
        return new TransferReceipt(operationType, journalEntryId, journalReference,
                sourceAccountId, destinationAccountId, amount, currency,
                sourceResultingBalance, destinationResultingBalance, postedAt, value);
    }

    private static BigDecimal normalize(BigDecimal value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        return value.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);
    }
}
