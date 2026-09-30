package com.bankingsystem.core.features.transactions.retry;

/** Small, bounded pause between failed transaction attempts. */
@FunctionalInterface
public interface FinancialTransactionRetryBackoff {
    void sleep(long millis);
}
