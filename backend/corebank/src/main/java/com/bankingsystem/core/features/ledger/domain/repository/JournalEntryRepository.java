package com.bankingsystem.core.features.ledger.domain.repository;

import com.bankingsystem.core.features.ledger.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByEntryReference(String entryReference);

    boolean existsByEntryReference(String entryReference);
}
