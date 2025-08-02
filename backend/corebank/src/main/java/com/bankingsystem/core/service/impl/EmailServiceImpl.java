package com.bankingsystem.core.service.impl;

import com.bankingsystem.core.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Please Verify Your Email";
        String verificationUrl = "http://localhost:8080/api/v1/auth/verify-email?token=" + token;
        String message = "Click the link to verify your email: " + verificationUrl;

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailMessage.setFrom("your-email@gmail.com");

        mailSender.send(mailMessage);
    }
}
