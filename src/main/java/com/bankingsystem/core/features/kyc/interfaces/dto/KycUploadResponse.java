package com.bankingsystem.core.features.kyc.interfaces.dto;

public class KycUploadResponse {
    private String id;
    public KycUploadResponse() {}
    public KycUploadResponse(String id) { this.id = id; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
