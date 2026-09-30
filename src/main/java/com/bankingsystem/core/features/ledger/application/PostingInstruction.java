package com.bankingsystem.core.features.ledger.application;

import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;
import com.bankingsystem.core.features.ledger.domain.PostingDirection;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable instruction for a single leg within a {@link PostingCommand}.
 */
public final class PostingInstruction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID ledgerAccountId;
    private final PostingDirection direction;
    private final MonetaryAmount amount;

    public PostingInstruction(UUID ledgerAccountId, PostingDirection direction, MonetaryAmount amount) {
        this.ledgerAccountId = Objects.requireNonNull(ledgerAccountId, "ledgerAccountId cannot be null");
        this.direction = Objects.requireNonNull(direction, "direction cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
    }

    public UUID getLedgerAccountId() {
        return ledgerAccountId;
    }

    public PostingDirection getDirection() {
        return direction;
    }

    public MonetaryAmount getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostingInstruction that = (PostingInstruction) o;
        return Objects.equals(ledgerAccountId, that.ledgerAccountId) &&
                direction == that.direction &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ledgerAccountId, direction, amount);
    }

    @Override
    public String toString() {
        return "PostingInstruction[" + direction + " " + amount + " -> " + ledgerAccountId + "]";
    }
}
