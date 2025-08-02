package com.bankingsystem.core.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
}
