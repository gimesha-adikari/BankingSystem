package com.bankingsystem.core.features.transactions.application;

import java.util.UUID;

/**
 * Internal customer-to-customer transfer workflow. Public HTTP wiring is intentionally separate.
 */
public interface TransferWorkflow {

    TransferReceipt transfer(UUID authenticatedUserId, UUID sourceAccountId,
                             UUID destinationAccountId, String decimalAmount,
                             String idempotencyKey);
}
