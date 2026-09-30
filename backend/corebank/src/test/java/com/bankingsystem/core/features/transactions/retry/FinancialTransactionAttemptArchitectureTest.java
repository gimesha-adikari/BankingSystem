package com.bankingsystem.core.features.transactions.retry;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialTransactionAttemptArchitectureTest {
    @Test
    void attemptIsASeparateRequiresNewTransactionalBoundary() throws Exception {
        Method method = FinancialTransactionAttempt.class.getMethod("execute", java.util.function.Supplier.class);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
