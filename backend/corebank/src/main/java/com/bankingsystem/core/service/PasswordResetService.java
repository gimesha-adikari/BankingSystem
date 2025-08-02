package com.bankingsystem.core.service;

public interface PasswordResetService {
    void initiateReset(String email);
    void resetPassword(String token, String newPassword);
}
