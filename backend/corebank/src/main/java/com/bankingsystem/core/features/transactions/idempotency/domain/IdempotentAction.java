package com.bankingsystem.core.features.transactions.idempotency.domain;

/**
 * Functional callback representing a financial action executed under an idempotency claim.
 */
@FunctionalInterface
public interface IdempotentAction {
    IdempotentCommandResult execute();
}
