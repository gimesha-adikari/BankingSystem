package com.bankingsystem.core.features.ledger.application;

/**
 * Authoritative transactional posting engine for double-entry financial events.
 *
 * <p>INTERNAL USE ONLY — No public or REST financial workflows are exposed through this engine
 * in Slice 4B-3.
 */
public interface PostingEngine {

    /**
     * Post a balanced double-entry transaction.
     *
     * @param command the posting command
     * @return the immutable PostingResult
     * @throws IllegalArgumentException if validation fails
     * @throws com.bankingsystem.core.modules.common.exceptions.BusinessException if business invariants are violated
     */
    PostingResult post(PostingCommand command);
}
