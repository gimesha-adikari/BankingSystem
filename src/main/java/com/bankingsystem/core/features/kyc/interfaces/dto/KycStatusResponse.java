package com.bankingsystem.core.features.kyc.interfaces.dto;

public class KycStatusResponse {
    private String requestId;
    private String status;
    public KycStatusResponse() {}
    public KycStatusResponse(String requestId, String status) {
        this.requestId = requestId; this.status = status;
    }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
