package com.bankingsystem.core.features.ledger.application;

import com.bankingsystem.core.features.ledger.domain.CurrencyCode;
import com.bankingsystem.core.features.ledger.domain.JournalEntryType;
import com.bankingsystem.core.features.ledger.domain.LedgerChannel;
import com.bankingsystem.core.features.ledger.domain.PostingActor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable command to post a balanced double-entry financial transaction.
 *
 * <p>Callers provide instructions; total amounts, references, timestamps, and sequence numbers
 * are strictly controlled and derived by the {@link PostingEngine}.
 */
public final class PostingCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final JournalEntryType entryType;
    private final CurrencyCode currency;
    private final String description;
    private final PostingActor actor;
    private final LedgerChannel channel;
    private final List<PostingInstruction> instructions;

    public PostingCommand(JournalEntryType entryType, CurrencyCode currency, String description,
                          PostingActor actor, LedgerChannel channel, List<PostingInstruction> instructions) {
        this.entryType = Objects.requireNonNull(entryType, "entryType cannot be null");
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
        this.description = description;
        this.actor = Objects.requireNonNull(actor, "actor cannot be null");
        this.channel = Objects.requireNonNull(channel, "channel cannot be null");
        if (instructions == null || instructions.size() < 2) {
            throw new IllegalArgumentException("Posting command must contain at least 2 instructions, got: "
                    + (instructions == null ? "null" : instructions.size()));
        }
        this.instructions = Collections.unmodifiableList(instructions);
    }

    public JournalEntryType getEntryType() {
        return entryType;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public PostingActor getActor() {
        return actor;
    }

    public LedgerChannel getChannel() {
        return channel;
    }

    public List<PostingInstruction> getInstructions() {
        return instructions;
    }
}
