package com.bankingsystem.core.features.auth.application;

public interface PasswordResetService {
    void initiateReset(String email);
    void resetPassword(String token, String newPassword);
}
