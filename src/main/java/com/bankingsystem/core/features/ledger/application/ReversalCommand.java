package com.bankingsystem.core.features.ledger.application;

import com.bankingsystem.core.features.ledger.domain.LedgerChannel;
import com.bankingsystem.core.features.ledger.domain.LedgerActorType;
import com.bankingsystem.core.features.ledger.domain.PostingActor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Trusted internal command for a complete reversal of one posted journal entry.
 * Accounting legs, amount, currency, and accounts are deliberately absent: they
 * are loaded from the immutable original journal by the posting engine.
 */
public final class ReversalCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID originalJournalEntryId;
    private final PostingActor actor;
    private final LedgerChannel channel;
    private final String reason;

    public ReversalCommand(UUID originalJournalEntryId, PostingActor actor,
                           LedgerChannel channel, String reason) {
        this.originalJournalEntryId = Objects.requireNonNull(
                originalJournalEntryId, "Original journal entry ID cannot be null");
        this.actor = Objects.requireNonNull(actor, "Reversal actor cannot be null");
        if (actor.getActorType() != LedgerActorType.USER || actor.getUserId() == null) {
            throw new IllegalArgumentException("Internal reversal requires a USER actor");
        }
        this.channel = Objects.requireNonNull(channel, "Reversal channel cannot be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reversal reason cannot be null or blank");
        }
        String normalized = reason.trim();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isISOControl(ch)) {
                throw new IllegalArgumentException("Reversal reason cannot contain control characters");
            }
        }
        this.reason = normalized;
    }

    public UUID getOriginalJournalEntryId() {
        return originalJournalEntryId;
    }

    public PostingActor getActor() {
        return actor;
    }

    public LedgerChannel getChannel() {
        return channel;
    }

    public String getReason() {
        return reason;
    }
}
