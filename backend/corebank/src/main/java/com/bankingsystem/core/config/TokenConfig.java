package com.bankingsystem.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenConfig {

    private final AppProperties appProperties;

    public TokenConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public int getExpirationHours() {
        return appProperties.getToken().getExpirationHours();
    }
}
