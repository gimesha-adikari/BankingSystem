package com.bankingsystem.core.features.wallet.interfaces.dto;

import com.bankingsystem.core.features.wallet.domain.entity.WalletCard;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardDto {
    private String id;
    private String brand;
    private String last4;
    private boolean isDefault;
    private OffsetDateTime createdAt;

    public static CardDto from(@NonNull WalletCard c) {
        return CardDto.builder()
                .id(c.getId().toString())
                .brand(c.getBrand())
                .last4(c.getLast4())
                .isDefault(c.isDefault())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
