package com.bankingsystem.core.features.ledger.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable JPA entity mapping for {@code journal_postings} table introduced in V2.
 *
 * <p>Represents an individual debit or credit leg of a double-entry journal entry.
 * Marked with {@link Immutable}; setters are omitted to enforce append-only financial facts.
 */
@Entity
@Table(name = "journal_postings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_journal_postings_entry_seq", columnNames = {"entry_id", "sequence_number"})
}, indexes = {
        @Index(name = "idx_journal_postings_account_created", columnList = "ledger_account_id, created_at")
})
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalPosting {

    @Id
    @Column(name = "posting_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID postingId;

    @Column(name = "entry_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID entryId;

    @Column(name = "ledger_account_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID ledgerAccountId;

    @Column(name = "sequence_number", updatable = false, nullable = false)
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", updatable = false, nullable = false, length = 10)
    private PostingDirection direction;

    @Column(name = "amount", updatable = false, nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", updatable = false, nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public JournalPosting(UUID postingId, UUID entryId, UUID ledgerAccountId,
                          Integer sequenceNumber, PostingDirection direction,
                          BigDecimal amount, String currency, LocalDateTime createdAt) {
        this.postingId = postingId;
        this.entryId = entryId;
        this.ledgerAccountId = ledgerAccountId;
        this.sequenceNumber = sequenceNumber;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }
}
