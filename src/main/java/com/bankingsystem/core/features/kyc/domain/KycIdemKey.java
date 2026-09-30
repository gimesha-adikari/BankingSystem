package com.bankingsystem.core.features.kyc.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(
        name = "kyc_idem_keys",
        uniqueConstraints = @UniqueConstraint(name = "uk_kik_user_idem_key", columnNames = {"user_id","idem_key"}),
        indexes = @Index(name = "idx_kik_created_at", columnList = "created_at")
)
public class KycIdemKey {
    @Id
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "idem_key", length = 100, nullable = false)
    private String idemKey;

    @Column(name = "case_id", length = 36, nullable = false)
    private String caseId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
