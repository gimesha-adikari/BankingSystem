package com.bankingsystem.core.features.kyc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "kyc_checks",
        indexes = {
                @Index(name = "idx_kyc_checks_case", columnList = "case_id"),
                @Index(name = "idx_kyc_checks_type", columnList = "type")
        })
public class KycCheck {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "case_id", length = 36, nullable = false)
    private String caseId;

    @Column(nullable = false, length = 40)
    private String type; // LIVENESS, FACE_MATCH, OCR_ID, DOC_CLASS

    private Double score;           // 0..1
    private Boolean passed;

    @Lob
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }
}
