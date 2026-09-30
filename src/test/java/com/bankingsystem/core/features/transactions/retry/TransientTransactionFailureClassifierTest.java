package com.bankingsystem.core.features.transactions.retry;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class TransientTransactionFailureClassifierTest {

    @Test
    void recognizesSqlState40001ThroughTranslatedCause() {
        SQLException deadlock = new SQLException("deadlock", "40001", 1213);
        assertThat(TransientTransactionFailureClassifier.isRetryable(
                new RuntimeException(new DataIntegrityViolationException("translated", deadlock)))).isTrue();
    }

    @Test
    void recognizesMysqlDeadlockCodeEvenWhenSqlStateDiffers() {
        assertThat(TransientTransactionFailureClassifier.isRetryable(
                new SQLException("deadlock", "HY000", 1213))).isTrue();
    }

    @Test
    void excludesLockWaitTimeoutAndIntegrityFailures() {
        assertThat(TransientTransactionFailureClassifier.isRetryable(
                new SQLException("lock wait timeout", "HY000", 1205))).isFalse();
        assertThat(TransientTransactionFailureClassifier.isRetryable(
                new SQLException("lock wait timeout", "40001", 1205))).isFalse();
        SQLException outer = new SQLException("outer", "40001", 0);
        outer.initCause(new SQLException("lock wait timeout", "40001", 1205));
        assertThat(TransientTransactionFailureClassifier.isRetryable(outer)).isFalse();
        assertThat(TransientTransactionFailureClassifier.isRetryable(
                new DataIntegrityViolationException("duplicate key"))).isFalse();
        assertThat(TransientTransactionFailureClassifier.isRetryable(new RuntimeException("business"))).isFalse();
    }
}
