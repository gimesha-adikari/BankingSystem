package com.bankingsystem.core.features.transactions.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Supplier;

/** Executes the complete idempotency + financial command in bounded fresh transactions. */
@Service
@Slf4j
public class BoundedFinancialTransactionRetryExecutor {

    public static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MILLIS = {25L, 50L};

    private final FinancialTransactionAttempt transactionAttempt;
    private final FinancialTransactionRetryBackoff backoff;

    @Autowired
    public BoundedFinancialTransactionRetryExecutor(
            FinancialTransactionAttempt transactionAttempt,
            FinancialTransactionRetryBackoff backoff) {
        this.transactionAttempt = transactionAttempt;
        this.backoff = backoff;
    }

    /** Convenience constructor for focused unit tests. */
    public BoundedFinancialTransactionRetryExecutor(FinancialTransactionAttempt transactionAttempt) {
        this(transactionAttempt, millis -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new FinancialTransactionRetryExhaustedException(
                        "interrupted-backoff", 0, interrupted);
            }
        });
    }

    public <T> T execute(String operation, Supplier<T> command) {
        Objects.requireNonNull(operation, "Operation must not be null");
        Objects.requireNonNull(command, "Command must not be null");

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return transactionAttempt.execute(command);
            } catch (RuntimeException failure) {
                if (!TransientTransactionFailureClassifier.isRetryable(failure)) {
                    throw failure;
                }
                if (attempt == MAX_ATTEMPTS) {
                    throw new FinancialTransactionRetryExhaustedException(operation, attempt, failure);
                }
                log.warn("Retrying financial transaction operation={} attempt={} maxAttempts={} category={}",
                        operation, attempt, MAX_ATTEMPTS,
                        TransientTransactionFailureClassifier.category(failure));
                backoff.sleep(BACKOFF_MILLIS[attempt - 1]);
            }
        }
        throw new IllegalStateException("Unreachable retry loop termination");
    }
}
