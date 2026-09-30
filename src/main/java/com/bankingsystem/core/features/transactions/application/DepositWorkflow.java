package com.bankingsystem.core.features.transactions.application;

import java.util.UUID;

/**
 * Internal customer deposit workflow. Public HTTP wiring is intentionally separate.
 */
public interface DepositWorkflow {

    DepositReceipt deposit(UUID authenticatedUserId, UUID targetAccountId,
                           String decimalAmount, String idempotencyKey);
}
