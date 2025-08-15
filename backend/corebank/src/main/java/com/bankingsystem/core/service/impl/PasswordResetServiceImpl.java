package com.bankingsystem.core.service.impl;

import com.bankingsystem.core.config.AppProperties;
import com.bankingsystem.core.entity.PasswordResetToken;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.repository.PasswordResetTokenRepository;
import com.bankingsystem.core.repository.UserRepository;
import com.bankingsystem.core.service.AuthService;
import com.bankingsystem.core.service.EmailService;
import com.bankingsystem.core.service.PasswordResetService;
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

    /**
     * Initiates a password reset. Does not reveal whether the email exists.
     */
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


    /**
     * Resets the password using a valid token, then marks the token as used.
     */
    @Transactional
    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (Boolean.TRUE.equals(prt.isUsed()) || prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired or used");
        }

        // Server-side strength validation
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
