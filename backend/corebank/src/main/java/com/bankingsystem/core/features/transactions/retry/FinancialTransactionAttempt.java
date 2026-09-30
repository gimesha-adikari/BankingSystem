package com.bankingsystem.core.features.transactions.retry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.function.Supplier;

/** Owns one and only one fresh physical transaction attempt. */
@Service
public class FinancialTransactionAttempt {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T execute(Supplier<T> command) {
        return Objects.requireNonNull(command, "Command must not be null").get();
    }
}
