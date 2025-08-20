package com.bankingsystem.core.features.kyc.interfaces.dto;

public class KycDecisionRequest {
    private String decision; // APPROVED or REJECTED
    private String reason;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
