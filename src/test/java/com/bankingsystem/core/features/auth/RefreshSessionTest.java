package com.bankingsystem.core.features.auth;

import com.bankingsystem.core.features.accesscontrol.domain.Role;
import com.bankingsystem.core.features.auth.application.AuthService;
import com.bankingsystem.core.features.auth.application.PasswordResetService;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.PasswordResetTokenRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.auth.interfaces.AuthController;
import com.bankingsystem.core.modules.common.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RefreshSessionTest {

    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    JwtUtils jwtUtils = mock(JwtUtils.class);
    AuthService authService = mock(AuthService.class);
    UserRepository userRepository = mock(UserRepository.class);
    PasswordResetService resetService = mock(PasswordResetService.class);
    PasswordResetTokenRepository resetTokenRepository = mock(PasswordResetTokenRepository.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                authenticationManager,
                jwtUtils,
                authService,
                userRepository,
                resetService,
                resetTokenRepository
        );
    }

    @Test
    void refreshTokenCreatesNewSessionAndInvalidatesOldSession() {
        String oldToken = "valid-old-token";
        String newToken = "valid-new-token";
        String username = "customer1";
        String ip = "192.168.1.10";

        when(request.getRemoteAddr()).thenReturn(ip);
        when(jwtUtils.validateJwtToken(oldToken)).thenReturn(true);
        when(authService.isSessionValid(oldToken)).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken(oldToken)).thenReturn(username);

        Role role = new Role();
        role.setRoleName("CUSTOMER");
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtUtils.generateJwtToken(username, "CUSTOMER")).thenReturn(newToken);

        ResponseEntity<?> response = controller.refreshToken(
                Map.of("username", username),
                "Bearer " + oldToken,
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("token", newToken));

        // Must invalidate the old session
        verify(authService).logout(oldToken);

        // Must create a session for the refreshed token so JwtAuthFilter recognizes it
        verify(authService).createSession(newToken, username, ip);
    }

    @Test
    void refreshTokenRejectsExpiredServerSession() {
        String oldToken = "expired-old-token";
        when(jwtUtils.validateJwtToken(oldToken)).thenReturn(true);
        when(authService.isSessionValid(oldToken)).thenReturn(false);

        ResponseEntity<?> response = controller.refreshToken(
                Map.of("username", "customer1"),
                "Bearer " + oldToken,
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(authService, never()).logout(oldToken);
        verify(authService, never()).createSession(anyString(), anyString(), anyString());
    }
}
