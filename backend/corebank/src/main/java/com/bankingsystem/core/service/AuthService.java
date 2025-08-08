package com.bankingsystem.core.service;

import com.bankingsystem.core.dto.ChangePasswordRequest;
import com.bankingsystem.core.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    void register(RegisterRequest request);
    boolean verifyEmail(String token);
    void createSession(String token, String username, String ipAddress);
    void logout(String token);
    void validatePasswordStrength(String password);
    void changePassword(String username, ChangePasswordRequest request);
}
