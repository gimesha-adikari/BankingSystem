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
 * Immutable JPA entity mapping for {@code journal_entries} table introduced in V2.
 *
 * <p>Represents the authoritative header record of an immutable double-entry transaction event.
 * Marked with {@link Immutable}; setters are intentionally package-private or omitted to prevent
 * runtime mutation of financial records.
 */
@Entity
@Table(name = "journal_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_journal_entries_reference", columnNames = "entry_reference"),
        @UniqueConstraint(name = "uk_journal_entries_reversal_of", columnNames = "reversal_of_entry_id")
})
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEntry {

    @Id
    @Column(name = "entry_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID entryId;

    @Column(name = "entry_reference", updatable = false, nullable = false, length = 64)
    private String entryReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", updatable = false, nullable = false, length = 30)
    private JournalEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", updatable = false, nullable = false, length = 20)
    private JournalEntryStatus status;

    @Column(name = "currency", updatable = false, nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", updatable = false, nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "description", updatable = false, length = 255)
    private String description;

    @Column(name = "reversal_of_entry_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID reversalOfEntryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", updatable = false, nullable = false, length = 10)
    private LedgerActorType actorType;

    @Column(name = "initiated_by_user_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID initiatedByUserId;

    @Column(name = "system_actor_id", updatable = false, length = 50)
    private String systemActorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", updatable = false, nullable = false, length = 20)
    private LedgerChannel channel;

    @Column(name = "posted_at", updatable = false, nullable = false)
    private LocalDateTime postedAt;

    public JournalEntry(UUID entryId, String entryReference, JournalEntryType entryType,
                        JournalEntryStatus status, String currency, BigDecimal totalAmount,
                        String description, UUID reversalOfEntryId, LedgerActorType actorType,
                        UUID initiatedByUserId, String systemActorId, LedgerChannel channel,
                        LocalDateTime postedAt) {
        this.entryId = entryId;
        this.entryReference = entryReference;
        this.entryType = entryType;
        this.status = status;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.description = description;
        this.reversalOfEntryId = reversalOfEntryId;
        this.actorType = actorType;
        this.initiatedByUserId = initiatedByUserId;
        this.systemActorId = systemActorId;
        this.channel = channel;
        this.postedAt = postedAt;
    }
}
