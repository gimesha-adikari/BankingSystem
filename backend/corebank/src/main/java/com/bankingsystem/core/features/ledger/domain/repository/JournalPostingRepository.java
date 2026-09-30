package com.bankingsystem.core.features.ledger.domain.repository;

import com.bankingsystem.core.features.ledger.domain.JournalPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalPostingRepository extends JpaRepository<JournalPosting, UUID> {

    List<JournalPosting> findByEntryIdOrderBySequenceNumberAsc(UUID entryId);

    List<JournalPosting> findByLedgerAccountIdOrderByCreatedAtAsc(UUID ledgerAccountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN jp.direction = com.bankingsystem.core.features.ledger.domain.PostingDirection.CREDIT " +
            "THEN jp.amount ELSE -jp.amount END), 0.0000) " +
            "FROM JournalPosting jp WHERE jp.ledgerAccountId = :ledgerAccountId")
    BigDecimal calculateDerivedLiabilityBalance(@Param("ledgerAccountId") UUID ledgerAccountId);
}
