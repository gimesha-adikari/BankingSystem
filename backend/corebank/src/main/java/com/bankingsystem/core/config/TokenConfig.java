package com.bankingsystem.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenConfig {

    @Value("${app.token.expiration-hours}")
    private int expirationHours;

    public int getExpirationHours() {
        return expirationHours;
    }
}
