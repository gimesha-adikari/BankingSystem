package com.bankingsystem.core.features.transactions.retry;

import org.springframework.stereotype.Component;

/** Production backoff implementation; invoked only after an attempt has ended. */
@Component
public class DefaultFinancialTransactionRetryBackoff implements FinancialTransactionRetryBackoff {
    @Override
    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new FinancialTransactionRetryExhaustedException(
                    "interrupted-backoff", 0, interrupted);
        }
    }
}
