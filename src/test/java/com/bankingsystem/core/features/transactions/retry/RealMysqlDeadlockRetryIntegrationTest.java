package com.bankingsystem.core.features.transactions.retry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/** Real InnoDB deadlock proof. The probe table is created and removed by this test only. */
@SpringBootTest
class RealMysqlDeadlockRetryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private BoundedFinancialTransactionRetryExecutor retryExecutor;


    @BeforeEach
    void createProbeTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS retry_deadlock_probe");
        jdbcTemplate.execute("CREATE TABLE retry_deadlock_probe (id INT PRIMARY KEY, value INT NOT NULL) ENGINE=InnoDB");
        for (int id = 1; id <= 102; id++) {
            jdbcTemplate.update("INSERT INTO retry_deadlock_probe (id, value) VALUES (?, 0)", id);
        }
    }

    @AfterEach
    void dropProbeTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS retry_deadlock_probe");
    }

    @Test
    void realMysqlLockWaitTimeout1205IsNotRetried() throws Exception {
        CountDownLatch holderLocked = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Void> holder = pool.submit(() -> {
            new TransactionTemplate(transactionManager).execute(status -> {
                jdbcTemplate.queryForObject(
                        "SELECT value FROM retry_deadlock_probe WHERE id = 1 FOR UPDATE", Integer.class);
                holderLocked.countDown();
                await(releaseHolder);
                return null;
            });
            return null;
        });
        assertThat(holderLocked.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> contender = pool.submit(() -> retryExecutor.execute("LOCK_TIMEOUT_PROBE", () -> {
            attempts.incrementAndGet();
            jdbcTemplate.execute("SET SESSION innodb_lock_wait_timeout = 1");
            jdbcTemplate.queryForObject(
                    "SELECT value FROM retry_deadlock_probe WHERE id = 1 FOR UPDATE", Integer.class);
            return null;
        }));

        RuntimeException failure = null;
        try {
            contender.get(10, TimeUnit.SECONDS);
            fail("Expected MySQL lock wait timeout");
        } catch (ExecutionException executionFailure) {
            failure = (RuntimeException) executionFailure.getCause();
        } finally {
            releaseHolder.countDown();
            holder.get(10, TimeUnit.SECONDS);
            pool.shutdownNow();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(attempts).hasValue(1);
        assertThat(failure).isNotNull();
        assertThat(TransientTransactionFailureClassifier.isRetryable(failure)).isFalse();
        SQLException sql = findSqlException(failure);
        assertThat(sql != null).isTrue();
        System.out.println("REAL_MYSQL_LOCK_TIMEOUT sqlState=" + sql.getSQLState()
                + " errorCode=" + sql.getErrorCode() + " retryable=false");
        assertThat(sql.getErrorCode()).isEqualTo(1205);
        assertThat(sql.getSQLState()).isIn("HY000", "40001");
    }

    @Test
    void realInnoDbDeadlockVictimIsRetriedInFreshTransaction() throws Exception {
        CountDownLatch targetRowOneLocked = new CountDownLatch(1);
        CountDownLatch competitorRowTwoLocked = new CountDownLatch(1);
        AtomicInteger targetInvocations = new AtomicInteger();
        AtomicReference<RuntimeException> firstTargetFailure = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Void> competitor = pool.submit(() -> {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute(status -> {
                // Give the competitor a larger undo footprint so InnoDB chooses the
                // smaller target transaction as the deadlock victim deterministically.
                jdbcTemplate.update("UPDATE retry_deadlock_probe SET value = value + 1 WHERE id >= 3");
                jdbcTemplate.queryForObject(
                        "SELECT value FROM retry_deadlock_probe WHERE id = 2 FOR UPDATE", Integer.class);
                competitorRowTwoLocked.countDown();
                await(targetRowOneLocked);
                jdbcTemplate.queryForObject(
                        "SELECT value FROM retry_deadlock_probe WHERE id = 1 FOR UPDATE", Integer.class);
                return null;
            });
            return null;
        });

        Future<Integer> target = pool.submit(() -> {
            try {
                return retryExecutor.execute("DEADLOCK_PROBE", () -> {
                    int invocation = targetInvocations.incrementAndGet();
                    try {
                        jdbcTemplate.update("UPDATE retry_deadlock_probe SET value = value + 1 WHERE id = 1");
                        targetRowOneLocked.countDown();
                        await(competitorRowTwoLocked);
                        jdbcTemplate.queryForObject(
                                "SELECT value FROM retry_deadlock_probe WHERE id = 2 FOR UPDATE", Integer.class);
                        return invocation;
                    } catch (RuntimeException failure) {
                        if (invocation == 1) {
                            firstTargetFailure.compareAndSet(null, failure);
                        }
                        throw failure;
                    }
                });
            } catch (RuntimeException failure) {
                firstTargetFailure.compareAndSet(null, failure);
                throw failure;
            }
        });

        Integer successfulInvocation;
        try {
            successfulInvocation = target.get(20, TimeUnit.SECONDS);
            competitor.get(20, TimeUnit.SECONDS);
        } catch (ExecutionException executionFailure) {
            throw new AssertionError("Deadlock probe failed", executionFailure.getCause());
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        // The target must have lost the real deadlock once and succeeded on attempt two.
        assertThat(successfulInvocation).isEqualTo(2);
        assertThat(targetInvocations).hasValue(2);
        assertThat(firstTargetFailure).hasValueSatisfying(failure -> {
            assertThat(TransientTransactionFailureClassifier.isRetryable(failure)).isTrue();
            SQLException sql = findSqlException(failure);
            assertThat(sql != null).isTrue();
            assertThat(sql.getErrorCode()).isEqualTo(1213);
            System.out.println("REAL_MYSQL_DEADLOCK sqlState=" + sql.getSQLState()
                    + " errorCode=" + sql.getErrorCode() + " retryable=true");
        });

        // Competitor updates committed; target's first update rolled back and only
        // its second attempt remains.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT value FROM retry_deadlock_probe WHERE id = 1", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT value FROM retry_deadlock_probe WHERE id = 3", Integer.class)).isEqualTo(1);
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

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("Deadlock probe barrier timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Deadlock probe was interrupted", interrupted);
        }
    }
}
