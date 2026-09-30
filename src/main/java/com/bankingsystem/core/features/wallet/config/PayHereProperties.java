package com.bankingsystem.core.features.wallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payhere")
public class PayHereProperties {
    private String merchantId;
    private String merchantSecret;
    private boolean sandbox = true;
    private String serverBaseUrl;
    private String appDeeplink;
    private String appCardDeeplink;
}
