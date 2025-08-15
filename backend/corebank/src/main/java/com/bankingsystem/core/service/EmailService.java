// src/main/java/com/bankingsystem/core/service/EmailService.java
package com.bankingsystem.core.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
    void sendEmail(String toEmail, String subject, String body);

    void sendEmailHtml(String toEmail, String subject, String html);

    /** Convenience for password reset emails using a template */
    void sendResetPasswordEmail(String toEmail, String resetLink, long expiryMinutes);

    /** Optional convenience for email change flow (token or full link) */
    default void sendEmailChangeVerification(String toEmail, String tokenOrLink) {
    }
}
