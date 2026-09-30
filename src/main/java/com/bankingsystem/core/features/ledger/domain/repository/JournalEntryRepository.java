package com.bankingsystem.core.features.ledger.domain.repository;

import com.bankingsystem.core.features.ledger.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JournalEntry j where j.entryId = :entryId")
    Optional<JournalEntry> findByIdForUpdate(@Param("entryId") UUID entryId);

    Optional<JournalEntry> findByEntryReference(String entryReference);

    boolean existsByEntryReference(String entryReference);

    Optional<JournalEntry> findByReversalOfEntryId(UUID reversalOfEntryId);
}
