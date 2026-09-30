package com.bankingsystem.core.features.transactions.application;

import java.util.UUID;

/**
 * Internal customer withdrawal workflow. Public HTTP wiring is intentionally separate.
 */
public interface WithdrawalWorkflow {

    WithdrawalReceipt withdraw(UUID authenticatedUserId, UUID targetAccountId,
                               String decimalAmount, String idempotencyKey);
}
