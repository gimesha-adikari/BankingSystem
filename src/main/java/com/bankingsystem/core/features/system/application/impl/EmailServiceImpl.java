// src/main/java/com/bankingsystem/core/service/impl/EmailServiceImpl.java
package com.bankingsystem.core.features.system.application.impl;

import com.bankingsystem.core.modules.common.config.AppProperties;
import com.bankingsystem.core.features.system.application.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final AppProperties appProperties;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@yourdomain.com}")
    private String fromAddress;

    @Value("${app.brand.name:MyBank}")
    private String brandName;

    // ───────────────────────────────────────────────────────────────────────────

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String verifyLink = appProperties.getFrontendUrl() + "/api/v1/auth/verify-email?token=" + token;

        String html = renderTemplate(
                "verify-email",
                Map.of(
                        "brandName", brandName,
                        "verifyLink", verifyLink,
                        "frontendUrl", appProperties.getFrontendUrl()
                )
        );

        sendEmailHtml(toEmail, "Verify your email", html);
    }

    @Override
    public void sendResetPasswordEmail(String toEmail, String resetLink, long expiryMinutes) {
        String html = renderTemplate(
                "reset-password",
                Map.of(
                        "brandName", brandName,
                        "resetLink", resetLink,
                        "expiryMinutes", String.valueOf(expiryMinutes),
                        "frontendUrl", appProperties.getFrontendUrl()
                )
        );
        sendEmailHtml(toEmail, "Reset your password", html);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(body);
        mailMessage.setFrom(fromAddress);
        mailSender.send(mailMessage);
    }

    @Override
    public void sendEmailHtml(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom(fromAddress);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // ───────────────────────────── Templates ─────────────────────────────

    /** Loads and renders templates from classpath: templates/email/{name}.html */
    private String renderTemplate(String name, Map<String, String> model) {
        String raw = readClasspathFile("templates/email/" + name + ".html");
        String rendered = raw;
        for (Map.Entry<String, String> e : model.entrySet()) {
            rendered = rendered.replace("${" + e.getKey() + "}", escapeHtml(e.getValue()));
        }
        return rendered;
    }

    private String readClasspathFile(String path) {
        try (Scanner scanner = new Scanner(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load email template: " + path, ex);
        }
    }

    /** Minimal HTML escape for text vars (keeps links safe). */
    private static String escapeHtml(@Nullable String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
