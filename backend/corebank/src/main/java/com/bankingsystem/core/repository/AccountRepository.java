package com.bankingsystem.core.repository;

import com.bankingsystem.core.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    // Fix method to navigate entity relationship properly:
    List<Account> findByCustomerCustomerId(UUID customerId);

    boolean existsByAccountNumber(String accountNumber);
}
