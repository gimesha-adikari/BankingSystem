package com.bankingsystem.core.features.auth.application.impl;

import com.bankingsystem.core.modules.common.config.AppProperties;
import com.bankingsystem.core.features.auth.domain.PasswordResetToken;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.PasswordResetTokenRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.auth.application.AuthService;
import com.bankingsystem.core.features.system.application.EmailService;
import com.bankingsystem.core.features.auth.application.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final AuthService authService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    @Override
    public void initiateReset(String email) {
        final String normalizedEmail = email == null ? "" : email.trim();
        Optional<User> userOpt = userRepo.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            addSmallRandomDelay();
            return;
        }

        User user = userOpt.get();

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        PasswordResetToken prt = tokenRepo.findByUserForUpdate(user)
                .orElseGet(() -> {
                    PasswordResetToken t = new PasswordResetToken();
                    t.setUser(user);
                    return t;
                });

        prt.setToken(token);
        prt.setExpiryDate(expiry);
        prt.setUsed(false);
        tokenRepo.save(prt);

        String frontend = appProperties.getFrontendUrl();
        String base = frontend != null && frontend.endsWith("/") ? frontend.substring(0, frontend.length() - 1) : frontend;
        String resetLink = base + "/reset-password?token=" + token;

        emailService.sendResetPasswordEmail(user.getEmail(), resetLink, 60L);
    }

    @Transactional
    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (Boolean.TRUE.equals(prt.isUsed()) || prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired or used");
        }

        authService.validatePasswordStrength(newPassword);

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        prt.setUsed(true);
        tokenRepo.save(prt);

        log.info("Password reset completed for userId={}", user.getUserId());
    }

    private void addSmallRandomDelay() {
        try {
            long delay = 150L + (long) (Math.random() * 200L);
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
