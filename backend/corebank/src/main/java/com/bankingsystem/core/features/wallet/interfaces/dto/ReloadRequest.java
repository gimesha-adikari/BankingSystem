package com.bankingsystem.core.features.wallet.interfaces.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReloadRequest {
    private String msisdn;
    private AmountDto amount;
}
