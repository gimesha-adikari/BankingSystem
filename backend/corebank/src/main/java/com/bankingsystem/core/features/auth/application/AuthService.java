package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.features.auth.interfaces.dto.ChangePasswordRequest;
import com.bankingsystem.core.features.auth.interfaces.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    boolean verifyEmail(String token);
    void createSession(String token, String username, String ipAddress);
    void logout(String token);
    void validatePasswordStrength(String password);
    void changePassword(String username, ChangePasswordRequest request);
}
