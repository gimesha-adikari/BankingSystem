package com.bankingsystem.core.repository;

import com.bankingsystem.core.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByAccountAccountIdOrderByCreatedAtDesc(UUID accountId);
}
