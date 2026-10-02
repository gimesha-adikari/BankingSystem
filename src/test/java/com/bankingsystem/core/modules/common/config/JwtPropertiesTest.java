package com.bankingsystem.core.modules.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void missingSecretIsRejected() {
        JwtProperties properties = new JwtProperties();

        assertThatThrownBy(properties::validateForRuntime)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT signing configuration is missing");
    }

    @Test
    void blankSecretIsRejected() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("   ");

        assertThatThrownBy(properties::validateForRuntime)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT signing configuration is missing");
    }

    @Test
    void placeholderAndShortSecretsAreRejected() {
        JwtProperties placeholder = new JwtProperties();
        placeholder.setSecret("CHANGE_ME_TO_A_RANDOM_SECRET");
        JwtProperties shortSecret = new JwtProperties();
        shortSecret.setSecret("short");

        assertThatThrownBy(placeholder::validateForRuntime)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forbidden placeholder");
        assertThatThrownBy(shortSecret::validateForRuntime)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 UTF-8 bytes");
    }

    @Test
    void validSecretIsAccepted() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-012345678901234567890123");

        properties.validateForRuntime();
    }
}
