package com.bankingsystem.core.features.ledger.domain.repository;

import com.bankingsystem.core.features.ledger.domain.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByCustomerAccountId(UUID customerAccountId);

    Optional<LedgerAccount> findBySystemCode(String systemCode);

    boolean existsBySystemCode(String systemCode);
}
