package com.bankingsystem.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String baseUrl;
    private Token token;
    private DefaultAdmin defaultAdmin;

    @Getter
    @Setter
    public static class Token {
        private int expirationHours;
    }

    @Getter
    @Setter
    public static class DefaultAdmin {
        private String username;
        private String email;
        private String password;
    }
}
