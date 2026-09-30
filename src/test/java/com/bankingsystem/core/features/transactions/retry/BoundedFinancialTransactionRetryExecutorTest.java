package com.bankingsystem.core.features.transactions.retry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoundedFinancialTransactionRetryExecutorTest {

    @Mock
    private FinancialTransactionAttempt attempt;

    @Test
    void retriesWholeCommandAndStopsAfterSuccess() {
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            int call = calls.incrementAndGet();
            if (call == 1) {
                throw new RuntimeException(new SQLException("deadlock", "40001", 1213));
            }
            return "ok";
        }).when(attempt).execute(any());

        String result = new BoundedFinancialTransactionRetryExecutor(attempt).execute("DEPOSIT", () -> "command");

        assertThat(result).isEqualTo("ok");
        verify(attempt, times(2)).execute(any());
    }

    @Test
    void propagatesBusinessFailureWithoutRetry() {
        doAnswer(invocation -> { throw new IllegalArgumentException("insufficient funds"); })
                .when(attempt).execute(any());

        assertThatThrownBy(() -> new BoundedFinancialTransactionRetryExecutor(attempt)
                .execute("WITHDRAWAL", () -> "command"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(attempt, times(1)).execute(any());
    }

    @Test
    void exhaustionPreservesFinalDeadlockCauseAndUsesThreeAttempts() {
        SQLException deadlock = new SQLException("deadlock", "40001", 1213);
        doAnswer(invocation -> { throw new RuntimeException(deadlock); })
                .when(attempt).execute(any());

        assertThatThrownBy(() -> new BoundedFinancialTransactionRetryExecutor(attempt)
                .execute("TRANSFER", () -> "command"))
                .isInstanceOf(FinancialTransactionRetryExhaustedException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .cause().hasCause(deadlock);
        verify(attempt, times(BoundedFinancialTransactionRetryExecutor.MAX_ATTEMPTS)).execute(any());
    }
}
