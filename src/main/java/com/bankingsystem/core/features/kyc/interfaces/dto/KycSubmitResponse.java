package com.bankingsystem.core.features.kyc.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class KycSubmitResponse {
    private String status;
    private String caseId;

    public KycSubmitResponse() {}
    public KycSubmitResponse(String status, String caseId) {
        this.status = status;
        this.caseId = caseId;
    }

}
