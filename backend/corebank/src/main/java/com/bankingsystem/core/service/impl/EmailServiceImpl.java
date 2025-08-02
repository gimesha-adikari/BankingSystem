package com.bankingsystem.core.service.impl;

import com.bankingsystem.core.config.AppProperties;
import com.bankingsystem.core.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private final AppProperties appProperties;
    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Please Verify Your Email";
        String verificationUrl = appProperties.getBaseUrl()+"/api/v1/auth/verify-email?token=" + token;
        String message = "Click the link to verify your email: " + verificationUrl;

        sendEmail(toEmail, subject, message);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(body);
        mailMessage.setFrom(appProperties.getBaseUrl());

        mailSender.send(mailMessage);
    }

    public void sendEmailChangeVerification(String to, String token) {
        String verificationUrl = appProperties.getBaseUrl()+"/api/v1/users/me/email/verify?token=" + token;
        String subject = "Verify your new email address";
        String body = "Click the following link to confirm your email change: " + verificationUrl;

        sendEmail(to, subject, body);
    }


}
