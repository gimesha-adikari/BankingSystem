package com.bankingsystem.core.features.wallet.interfaces.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardListResponse {
    private List<CardDto> cards;
}
