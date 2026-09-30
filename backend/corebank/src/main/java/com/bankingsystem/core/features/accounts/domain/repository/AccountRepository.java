package com.bankingsystem.core.features.accounts.domain.repository;

import com.bankingsystem.core.features.accounts.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    // Fix method to navigate entity relationship properly:
    List<Account> findByCustomerCustomerId(UUID customerId);

    boolean existsByAccountNumber(String accountNumber);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
    java.util.Optional<Account> findByIdForUpdate(@org.springframework.data.repository.query.Param("accountId") UUID accountId);
}
