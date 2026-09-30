package com.bankingsystem.core.features.wallet.interfaces.dto;

import com.bankingsystem.core.features.wallet.domain.PaymentStatus;
import com.bankingsystem.core.features.wallet.domain.entity.PaymentIntent;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentIntentDto {
    private String intentId;
    private PaymentStatus status;
    private AmountDto amount;
    private String description;
    private String returnUrl;
    private String providerClientSecret;

    public static PaymentIntentDto from(PaymentIntent p) {
        return PaymentIntentDto.builder()
                .intentId(p.getId())
                .status(p.getStatus())
                .amount(new AmountDto(p.getAmountValue(), p.getAmountCurrency()))
                .description(p.getDescription())
                .returnUrl(p.getReturnUrl())
                .providerClientSecret(p.getProviderClientSecret())
                .build();
    }
}
