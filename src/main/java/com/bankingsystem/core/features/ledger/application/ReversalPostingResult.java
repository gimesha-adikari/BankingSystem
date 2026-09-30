package com.bankingsystem.core.features.ledger.application;

import java.io.Serializable;
import java.util.Objects;

/** Result of a reversal attempt, including whether it was a natural replay. */
public final class ReversalPostingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PostingResult postingResult;
    private final boolean replayed;

    public ReversalPostingResult(PostingResult postingResult, boolean replayed) {
        this.postingResult = Objects.requireNonNull(postingResult, "Posting result cannot be null");
        this.replayed = replayed;
    }

    public PostingResult getPostingResult() {
        return postingResult;
    }

    public boolean isReplayed() {
        return replayed;
    }
}
