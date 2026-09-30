package com.bankingsystem.core.features.wallet.integration.psp;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class FakePspClient implements PspClient {
    @Override
    public String createAddCardSessionUrl(String sessionId, String returnUrl) {
        return returnUrl + "?sessionId=" + sessionId;
    }
    @Override
    public String createProviderClientSecret(double amount, String currency, String description) {
        return "cs_" + UUID.randomUUID();
    }
}
