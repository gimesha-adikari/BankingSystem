package com.bankingsystem.core.features.wallet.interfaces.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AmountDto {
    private double value;
    private String currency;
}
