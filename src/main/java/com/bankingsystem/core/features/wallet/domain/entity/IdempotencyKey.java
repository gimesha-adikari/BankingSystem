package com.bankingsystem.core.features.wallet.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "wallet_idempotency")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyKey {
    @Id
    @Column(name = "idem_key", nullable = false, length = 255)
    private String idemKey;

    @Column(name = "request_hash", nullable = false, length = 255)
    private String requestHash;

    @Lob
    @Column(name = "response_json", columnDefinition = "LONGTEXT", nullable = false)
    private String responseJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
