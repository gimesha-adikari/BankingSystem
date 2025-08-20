package com.bankingsystem.core.features.kyc.interfaces.dto;

public class KycSubmitResponse {
    private String status;   // e.g., PENDING
    private String requestId;

    public KycSubmitResponse() {}
    public KycSubmitResponse(String status, String requestId) {
        this.status = status;
        this.requestId = requestId;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
