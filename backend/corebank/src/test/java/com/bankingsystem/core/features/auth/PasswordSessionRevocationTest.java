package com.bankingsystem.core.features.auth;

import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.auth.application.impl.AuthServiceImpl;
import com.bankingsystem.core.features.auth.application.impl.PasswordResetServiceImpl;
import com.bankingsystem.core.features.auth.domain.PasswordResetToken;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.PasswordResetTokenRepository;
import com.bankingsystem.core.features.auth.domain.repository.SessionRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.auth.domain.repository.VerificationTokenRepository;
import com.bankingsystem.core.features.auth.interfaces.dto.ChangePasswordRequest;
import com.bankingsystem.core.features.system.application.EmailService;
import com.bankingsystem.core.modules.common.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordSessionRevocationTest {

    UserRepository users = mock(UserRepository.class);
    RoleRepository roles = mock(RoleRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    SessionRepository sessions = mock(SessionRepository.class);
    VerificationTokenRepository vTokens = mock(VerificationTokenRepository.class);
    EmailService email = mock(EmailService.class);
    AppProperties props = new AppProperties();
    PasswordResetTokenRepository resetTokenRepo = mock(PasswordResetTokenRepository.class);

    AuthServiceImpl authService = new AuthServiceImpl(users, roles, encoder, sessions, vTokens, email, props);

    @Test
    void changePasswordRevokesAllUserSessions() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setUsername("alice");
        user.setPasswordHash("hashed-old");
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(encoder.matches("OldPass1!", "hashed-old")).thenReturn(true);
        when(encoder.encode(any())).thenReturn("hashed-new");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("OldPass1!");
        req.setNewPassword("NewPass1!");
        req.setConfirmNewPassword("NewPass1!");

        authService.changePassword("alice", req);

        verify(sessions).deleteByUserUserId(userId);
        verify(users).save(user);
    }

    @Test
    void passwordResetRevokesAllUserSessions() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setUsername("bob");

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken("reset-tok");
        prt.setUser(user);
        prt.setExpiryDate(LocalDateTime.now().plusHours(1));
        prt.setUsed(false);

        when(resetTokenRepo.findByToken("reset-tok")).thenReturn(Optional.of(prt));
        when(resetTokenRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        AppProperties appProps = new AppProperties();
        PasswordResetServiceImpl resetService = new PasswordResetServiceImpl(
                resetTokenRepo, users, email, appProps, authService);

        resetService.resetPassword("reset-tok", "NewPass2!");

        verify(sessions).deleteByUserUserId(userId);
    }
}
