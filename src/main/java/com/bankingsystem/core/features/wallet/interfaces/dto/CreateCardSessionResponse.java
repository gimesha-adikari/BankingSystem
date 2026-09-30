package com.bankingsystem.core.features.wallet.interfaces.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateCardSessionResponse {
    private String sessionId;
    private String url;
}
