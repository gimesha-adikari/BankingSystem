package com.bankingsystem.core.features.transactions.retry;

import java.sql.SQLException;

/**
 * Recognizes only database transaction rollback/deadlock failures that are safe
 * to retry as a complete transaction. Ordinary data-access failures are not
 * retryable. MySQL error 1205 is explicitly excluded even when its driver
 * reports SQLSTATE 40001.
 */
public final class TransientTransactionFailureClassifier {

    private TransientTransactionFailureClassifier() {
    }

    public static boolean isRetryable(Throwable failure) {
        boolean deadlockSignal = false;
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                // MySQL lock wait timeout is not a deadlock for this slice. Scan
                // the complete chain so it also overrides an outer 40001 wrapper.
                if (sqlException.getErrorCode() == 1205) {
                    return false;
                }
                if (sqlException.getErrorCode() == 1213
                        || "40001".equals(sqlException.getSQLState())) {
                    deadlockSignal = true;
                }
            }
            current = current.getCause();
        }
        return deadlockSignal;
    }

    public static String category(Throwable failure) {
        return isRetryable(failure) ? "MYSQL_DEADLOCK_TRANSACTION_ROLLBACK" : "NON_RETRYABLE";
    }
}
