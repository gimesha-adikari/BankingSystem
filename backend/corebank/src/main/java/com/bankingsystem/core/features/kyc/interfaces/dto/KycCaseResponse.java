package com.bankingsystem.core.features.kyc.interfaces.dto;

import com.bankingsystem.core.features.kyc.domain.KycCase;

import java.time.Instant;

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
        return r;
    }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDocFrontId() { return docFrontId; }
    public void setDocFrontId(String docFrontId) { this.docFrontId = docFrontId; }
    public String getDocBackId() { return docBackId; }
    public void setDocBackId(String docBackId) { this.docBackId = docBackId; }
    public String getSelfieId() { return selfieId; }
    public void setSelfieId(String selfieId) { this.selfieId = selfieId; }
    public String getAddressId() { return addressId; }
    public void setAddressId(String addressId) { this.addressId = addressId; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
