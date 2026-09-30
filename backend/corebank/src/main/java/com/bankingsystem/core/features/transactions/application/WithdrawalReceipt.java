package com.bankingsystem.core.features.transactions.application;

import com.bankingsystem.core.features.ledger.domain.JournalEntryType;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Immutable, JSON-serializable receipt for a customer withdrawal. */
public record WithdrawalReceipt(
        JournalEntryType operationType,
        UUID journalEntryId,
        String journalReference,
        UUID accountId,
        BigDecimal amount,
        String currency,
        BigDecimal resultingBalance,
        LocalDateTime postedAt,
        boolean replayed
) {

    public WithdrawalReceipt {
        if (operationType != JournalEntryType.WITHDRAWAL) {
            throw new IllegalArgumentException("Withdrawal receipt must have WITHDRAWAL operation type");
        }
        Objects.requireNonNull(journalEntryId, "Journal entry ID must not be null");
        Objects.requireNonNull(journalReference, "Journal reference must not be null");
        Objects.requireNonNull(accountId, "Account ID must not be null");
        amount = normalize(amount, "Amount");
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be null or blank");
        }
        resultingBalance = normalize(resultingBalance, "Resulting balance");
        Objects.requireNonNull(postedAt, "Posted timestamp must not be null");
    }

    public WithdrawalReceipt withReplayed(boolean value) {
        return new WithdrawalReceipt(operationType, journalEntryId, journalReference, accountId,
                amount, currency, resultingBalance, postedAt, value);
    }

    private static BigDecimal normalize(BigDecimal value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        return value.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);
    }
}
