package com.bankingsystem.core.features.ledger.application;

import com.bankingsystem.core.features.ledger.domain.CurrencyCode;
import com.bankingsystem.core.features.ledger.domain.JournalEntryType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable result of a successful ledger posting execution.
 */
public final class PostingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID entryId;
    private final String entryReference;
    private final JournalEntryType entryType;
    private final CurrencyCode currency;
    private final BigDecimal totalAmount;
    private final LocalDateTime postedAt;
    private final Map<UUID, BigDecimal> resultingCustomerBalances;

    public PostingResult(UUID entryId, String entryReference, JournalEntryType entryType,
                         CurrencyCode currency, BigDecimal totalAmount, LocalDateTime postedAt,
                         Map<UUID, BigDecimal> resultingCustomerBalances) {
        this.entryId = Objects.requireNonNull(entryId, "entryId cannot be null");
        this.entryReference = Objects.requireNonNull(entryReference, "entryReference cannot be null");
        this.entryType = Objects.requireNonNull(entryType, "entryType cannot be null");
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        this.postedAt = Objects.requireNonNull(postedAt, "postedAt cannot be null");
        this.resultingCustomerBalances = resultingCustomerBalances != null
                ? Collections.unmodifiableMap(resultingCustomerBalances)
                : Collections.emptyMap();
    }

    public UUID getEntryId() {
        return entryId;
    }

    public String getEntryReference() {
        return entryReference;
    }

    public JournalEntryType getEntryType() {
        return entryType;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public Map<UUID, BigDecimal> getResultingCustomerBalances() {
        return resultingCustomerBalances;
    }

    @Override
    public String toString() {
        return "PostingResult[entryId=" + entryId + ", ref=" + entryReference + ", type=" + entryType +
                ", amount=" + totalAmount + " " + currency + "]";
    }
}
