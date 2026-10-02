package com.bankingsystem.core.features.auth.interfaces;

import com.bankingsystem.core.features.auth.application.AuthService;
import com.bankingsystem.core.features.auth.application.LoginAuthenticationService;
import com.bankingsystem.core.features.auth.application.LoginRateLimiter;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.PasswordResetTokenRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.auth.interfaces.dto.JwtResponse;
import com.bankingsystem.core.features.auth.interfaces.dto.LoginRequest;
import com.bankingsystem.core.features.auth.application.PasswordResetService;
import com.bankingsystem.core.modules.common.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginControllerContractTest {

    LoginAuthenticationService login = mock(LoginAuthenticationService.class);
    JwtUtils jwtUtils = mock(JwtUtils.class);
    AuthService authService = mock(AuthService.class);
    UserRepository users = mock(UserRepository.class);
    PasswordResetService resetService = mock(PasswordResetService.class);
    PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    AuthController controller = new AuthController(login, jwtUtils, authService, users, resetService, resetTokens);

    @BeforeEach
    void setUp() {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void expectedAuthenticationFailuresShareGeneric401Contract() {
        for (RuntimeException failure : new RuntimeException[]{
                new BadCredentialsException("bad"),
                new UsernameNotFoundException("missing"),
                new DisabledException("disabled"),
                new LockedException("locked")
        }) {
            doThrow(failure).when(login).authenticate(anyString(), anyString(), anyString());

            ResponseEntity<?> response = controller.authenticateUser(requestBody("alice", "wrong"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isEqualTo(Map.of("error", "Invalid username or password"));
            assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
        }
    }

    @Test
    void inactiveAccountFailureDoesNotCreateTokenOrSession() {
        doThrow(new DisabledException("inactive"))
                .when(login).authenticate(anyString(), anyString(), anyString());

        ResponseEntity<?> response = controller.authenticateUser(requestBody("alice", "password"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Invalid username or password"));
        verifyNoInteractions(jwtUtils, authService);
    }

    @Test
    void throttledLoginUsesGeneric429AndRetryAfter() {
        when(login.authenticate(anyString(), anyString(), anyString()))
                .thenThrow(new com.bankingsystem.core.features.auth.application.LoginRateLimiter.ThrottledException(12));

        ResponseEntity<?> response = controller.authenticateUser(requestBody("alice", "wrong"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Too many login attempts. Try again later."));
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("12");
        assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void limiterFailureReturnsGenericServiceUnavailable() {
        doThrow(new LoginRateLimiter.LimiterFailureException())
                .when(login).authenticate(anyString(), anyString(), anyString());

        ResponseEntity<?> response = controller.authenticateUser(requestBody("alice", "password"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Service temporarily unavailable"));
    }

    @Test
    void blankLoginFieldsRemain400AndDoNotCallAuthenticationService() {
        ResponseEntity<?> response = controller.authenticateUser(requestBody(" ", ""), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "code", "ERR_VALIDATION",
                "message", "Validation failed",
                "errors", Map.of("username", "must not be blank", "password", "must not be blank")
        ));
        verifyNoInteractions(login);
    }

    @Test
    void missingLoginFieldRemains400AndDoesNotReachAuthentication() {
        ResponseEntity<?> response = controller.authenticateUser(requestBody(null, "password"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "code", "ERR_VALIDATION",
                "message", "Validation failed",
                "errors", Map.of("username", "must not be blank")
        ));
        verifyNoInteractions(login);
    }

    @Test
    void successfulLoginPreservesJwtResponseAndSessionCreation() {
        User user = new User();
        user.setUsername("alice");
        com.bankingsystem.core.features.accesscontrol.domain.Role role = new com.bankingsystem.core.features.accesscontrol.domain.Role();
        role.setRoleName("CUSTOMER");
        user.setRole(role);
        Authentication authentication = mock(Authentication.class);
        when(login.authenticate("alice", "password", "10.0.0.1"))
                .thenReturn(new LoginAuthenticationService.LoginResult(authentication, user));
        when(jwtUtils.generateJwtToken("alice", "CUSTOMER")).thenReturn("jwt-token");

        ResponseEntity<?> response = controller.authenticateUser(requestBody("alice", "password"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new JwtResponse("jwt-token", "alice", "CUSTOMER"));
        verify(authService).createSession("jwt-token", "alice", "10.0.0.1");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test
    void unexpectedFailureIsGeneric500AndClearsContext() {
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        when(login.authenticate(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("internal"));

        ResponseEntity<?> response = controller.authenticateUser(requestBody("alice", "password"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Internal server error"));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static LoginRequest requestBody(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
