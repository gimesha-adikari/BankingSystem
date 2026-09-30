package com.bankingsystem.core.features.transactions.retry;

/** Terminal failure after all bounded fresh transaction attempts fail. */
public class FinancialTransactionRetryExhaustedException extends RuntimeException {
    public FinancialTransactionRetryExhaustedException(String operation, int attempts, Throwable cause) {
        super("Financial transaction retry exhausted for operation " + operation
                + " after " + attempts + " attempts", cause);
    }
}
