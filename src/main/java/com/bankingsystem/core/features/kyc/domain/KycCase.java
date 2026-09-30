package com.bankingsystem.core.features.kyc.domain;

import com.bankingsystem.core.modules.common.enums.KycStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "kyc_cases",
        indexes = {
                @Index(name = "idx_kyc_cases_user_id", columnList = "user_id"),
                @Index(name = "idx_kyc_cases_status", columnList = "status"),
                @Index(name = "idx_kyc_cases_user_created", columnList = "user_id,created_at")
        })
public class KycCase {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false, length = 36)
    private String docFrontId;

    @Column(nullable = false, length = 36)
    private String docBackId;

    @Column(nullable = false, length = 36)
    private String selfieId;

    @Column(nullable = false, length = 36)
    private String addressId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 500)
    private String decisionReason;

    @Version
    private long version;

    @Column(name = "reviewed_by", columnDefinition = "BINARY(16)")
    private UUID reviewedBy;

    private Instant decidedAt;

    @Column(length = 1000)
    private String reviewerNotes;

    @Column(name = "processing", nullable = false)
    private boolean processing = false;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null) this.status = KycStatus.PENDING;
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
