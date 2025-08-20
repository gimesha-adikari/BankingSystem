package com.bankingsystem.core.features.kyc.interfaces.dto;

public class KycSubmitRequest {
    private String docFrontId;
    private String docBackId;
    private String selfieId;
    private String addressId;
    private boolean consent;

    public String getDocFrontId() { return docFrontId; }
    public void setDocFrontId(String docFrontId) { this.docFrontId = docFrontId; }
    public String getDocBackId() { return docBackId; }
    public void setDocBackId(String docBackId) { this.docBackId = docBackId; }
    public String getSelfieId() { return selfieId; }
    public void setSelfieId(String selfieId) { this.selfieId = selfieId; }
    public String getAddressId() { return addressId; }
    public void setAddressId(String addressId) { this.addressId = addressId; }
    public boolean isConsent() { return consent; }
    public void setConsent(boolean consent) { this.consent = consent; }
}
