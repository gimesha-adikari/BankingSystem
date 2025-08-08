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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AppProperties appProperties;
    private final AuthService authService;

    @Override
    public void initiateReset(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        tokenRepo.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setUser(user);
        prt.setExpiryDate(expiry);
        tokenRepo.save(prt);

        String resetLink = appProperties.getFrontendUrl()+"/reset-password?token=" + token;
        emailService.sendEmail(user.getEmail(), "Reset Password", "Reset link: " + resetLink);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (prt.isUsed() || prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired or used");
        }

        authService.validatePasswordStrength( newPassword);

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        prt.setUsed(true);
        tokenRepo.save(prt);
    }
}
