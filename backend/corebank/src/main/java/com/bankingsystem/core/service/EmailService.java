package com.bankingsystem.core.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
    void sendEmail(String toEmail, String subject, String body);
}
