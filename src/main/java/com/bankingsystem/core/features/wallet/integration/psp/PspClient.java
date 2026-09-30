package com.bankingsystem.core.features.wallet.integration.psp;

public interface PspClient {
    String createAddCardSessionUrl(String sessionId, String returnUrl);
    String createProviderClientSecret(double amount, String currency, String description);
}
