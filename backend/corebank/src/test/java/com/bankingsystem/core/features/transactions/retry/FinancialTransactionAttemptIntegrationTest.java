package com.bankingsystem.core.features.transactions.retry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FinancialTransactionAttemptIntegrationTest {

    @Autowired
    private FinancialTransactionAttempt attempt;

    @Autowired
    private BoundedFinancialTransactionRetryExecutor retryExecutor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createProbeTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS retry_attempt_probe");
        jdbcTemplate.execute("CREATE TABLE retry_attempt_probe (id INT PRIMARY KEY, marker VARCHAR(32) NOT NULL) ENGINE=InnoDB");
    }

    @AfterEach
    void dropProbeTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS retry_attempt_probe");
    }

    @Test
    void attemptRunsInsideAndThenLeavesARealSpringTransaction() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        Integer result = attempt.execute(() -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            jdbcTemplate.update("INSERT INTO retry_attempt_probe (id, marker) VALUES (1, 'single')");
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM retry_attempt_probe", Integer.class);
        });

        assertThat(result).isEqualTo(1);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM retry_attempt_probe", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void retryExhaustionLeavesAllAttemptWritesRolledBack() {
        AtomicInteger invocations = new AtomicInteger();

        assertThatThrownBy(() -> retryExecutor.execute("EXHAUSTION_PROBE", () -> {
            int attemptNumber = invocations.incrementAndGet();
            jdbcTemplate.update("INSERT INTO retry_attempt_probe (id, marker) VALUES (?, ?)",
                    attemptNumber, "attempt-" + attemptNumber);
            throw new RuntimeException(new SQLException("deadlock", "40001", 1213));
        }))
                .isInstanceOf(FinancialTransactionRetryExhaustedException.class)
                .hasCauseInstanceOf(RuntimeException.class);

        assertThat(invocations).hasValue(BoundedFinancialTransactionRetryExecutor.MAX_ATTEMPTS);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM retry_attempt_probe", Integer.class))
                .isZero();
    }

    @Test
    void nonRetryableDuplicateKeyFailureRunsOnceAndRollsBack() {
        AtomicInteger invocations = new AtomicInteger();
        RuntimeException failure = null;

        try {
            retryExecutor.execute("DUPLICATE_KEY_PROBE", () -> {
                invocations.incrementAndGet();
                jdbcTemplate.update("INSERT INTO retry_attempt_probe (id, marker) VALUES (1, 'first')");
                jdbcTemplate.update("INSERT INTO retry_attempt_probe (id, marker) VALUES (1, 'duplicate')");
                return null;
            });
        } catch (RuntimeException thrown) {
            failure = thrown;
        }

        assertThat(failure).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(TransientTransactionFailureClassifier.isRetryable(failure)).isFalse();
        SQLException sql = findSqlException(failure);
        assertThat(sql != null).isTrue();
        System.out.println("NON_RETRYABLE_SQL sqlState=" + sql.getSQLState()
                + " errorCode=" + sql.getErrorCode() + " retryable=false");
        assertThat(sql.getSQLState()).isEqualTo("23000");
        assertThat(sql.getErrorCode()).isEqualTo(1062);
        assertThat(invocations).hasValue(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM retry_attempt_probe", Integer.class))
                .isZero();
    }

    private static SQLException findSqlException(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sql) {
                return sql;
            }
            current = current.getCause();
        }
        return null;
    }

    @Test
    void failedAttemptRollsBackAndRetryUsesFreshTransactionBoundary() {
        AtomicInteger invocations = new AtomicInteger();

        Integer result = retryExecutor.execute("TEST_PROBE", () -> {
            int attemptNumber = invocations.incrementAndGet();
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            jdbcTemplate.update("INSERT INTO retry_attempt_probe (id, marker) VALUES (?, ?)",
                    attemptNumber, "attempt-" + attemptNumber);
            if (attemptNumber == 1) {
                throw new RuntimeException(new SQLException("deadlock", "40001", 1213));
            }
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM retry_attempt_probe", Integer.class);
        });

        assertThat(result).isEqualTo(1);
        assertThat(invocations).hasValue(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM retry_attempt_probe", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT marker FROM retry_attempt_probe WHERE id = 2", String.class))
                .isEqualTo("attempt-2");
        assertThat(jdbcTemplate.query(
                "SELECT id FROM retry_attempt_probe ORDER BY id", (rs, rowNum) -> rs.getInt(1)))
                .containsExactly(2);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }
}
