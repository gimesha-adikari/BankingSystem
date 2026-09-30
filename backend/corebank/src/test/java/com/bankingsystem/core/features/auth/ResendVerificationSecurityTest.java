package com.bankingsystem.core.features.auth;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ResendVerificationSecurityTest {

    @Test
    void securityConfigPermitsUnauthenticatedResendVerification() throws Exception {
        Path configPath = Path.of("src/main/java/com/bankingsystem/core/modules/common/security/SecurityConfig.java");
        String content = Files.readString(configPath);

        assertThat(content)
                .withFailMessage("SecurityConfig must include /api/v1/auth/resend-verification in permitAll list")
                .contains("\"/api/v1/auth/resend-verification\"");
    }
}
