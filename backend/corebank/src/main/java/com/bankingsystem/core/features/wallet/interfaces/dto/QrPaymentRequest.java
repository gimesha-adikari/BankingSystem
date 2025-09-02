package com.bankingsystem.core.features.wallet.interfaces.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QrPaymentRequest {
    private AmountDto amount;
    private String qrData;
    private String merchantRef;
}
