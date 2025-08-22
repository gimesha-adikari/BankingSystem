package com.bankingsystem.core.features.kyc.interfaces.dto;

import com.bankingsystem.core.features.kyc.domain.KycCase;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycCaseResponse {
    private String caseId;
    private String status;
    private String docFrontId;
    private String docBackId;
    private String selfieId;
    private String addressId;
    private String decisionReason;
    private Instant createdAt;
    private Instant updatedAt;
    private String reviewerId;
    private Instant decidedAt;
    private String reviewerNotes;

    public static KycCaseResponse from(KycCase c) {
        KycCaseResponse r = new KycCaseResponse();
        r.caseId = c.getId();
        r.status = c.getStatus().name();
        r.docFrontId = c.getDocFrontId();
        r.docBackId = c.getDocBackId();
        r.selfieId = c.getSelfieId();
        r.addressId = c.getAddressId();
        r.decisionReason = c.getDecisionReason();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        r.reviewerId = c.getReviewedBy() != null ? c.getReviewedBy().toString() : null;
        r.decidedAt = c.getDecidedAt();
        r.reviewerNotes = c.getReviewerNotes();
        return r;
    }
}
