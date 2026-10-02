package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.features.auth.interfaces.dto.ChangePasswordRequest;
import com.bankingsystem.core.features.auth.interfaces.dto.RegisterRequest;

import java.util.UUID;

public interface AuthService {
    void register(RegisterRequest request);
    void resendVerification(String email);
    boolean verifyEmail(String token);
    void createSession(String token, String username, String ipAddress);
    boolean isSessionValid(String token);
    void logout(String token);
    void validatePasswordStrength(String password);
    void changePassword(String username, ChangePasswordRequest request);
    void revokeAllSessions(UUID userId);
}
