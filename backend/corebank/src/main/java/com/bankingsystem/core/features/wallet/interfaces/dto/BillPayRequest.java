package com.bankingsystem.core.features.wallet.interfaces.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillPayRequest {
    private String billerId;
    private String reference;
    private AmountDto amount;
}
