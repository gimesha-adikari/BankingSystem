package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoginAuthenticationServiceTest {

    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    UserRepository users = mock(UserRepository.class);
    LoginRateLimiter limiter = mock(LoginRateLimiter.class);
    LoginAuthenticationService service = new LoginAuthenticationService(authenticationManager, users, limiter);

    @BeforeEach
    void setUp() {
        when(limiter.tryAdmit(anyString(), anyString())).thenReturn(LoginRateLimiter.Admission.permitted());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unknownUserFailureDoesNotPerformControllerRepositoryPrelookup() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> service.authenticate("missing", "wrong", "10.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(users);
        verify(authenticationManager).authenticate(any(Authentication.class));
    }

    @Test
    void inactiveUserIsCheckedByPasswordPathBeforeGenericRejection() {
        User user = user(false);
        Authentication authentication = authentication("alice");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.authenticate("alice", "password", "10.0.0.1"))
                .isInstanceOf(DisabledException.class);

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(users).findByUsername("alice");
        verify(users, never()).save(any(User.class));
        verify(limiter, never()).resetAfterSuccessfulLogin(anyString(), anyString());
    }

    @Test
    void successfulLoginReturnsUserAndResetsOnlyAccountKeys() {
        User user = user(true);
        Authentication authentication = authentication("alice");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));

        LoginAuthenticationService.LoginResult result = service.authenticate(" alice ", "password", "10.0.0.1");

        assertThat(result.user()).isSameAs(user);
        assertThat(result.authentication()).isSameAs(authentication);
        verify(limiter).tryAdmit("10.0.0.1", " alice ");
        verify(limiter).resetAfterSuccessfulLogin("10.0.0.1", " alice ");
    }

    @Test
    void failedLoginClearsStaleSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(authentication("stale"));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> service.authenticate("alice", "wrong", "10.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void limiterRejectionPreventsAuthentication() {
        when(limiter.tryAdmit(anyString(), anyString()))
                .thenReturn(new LoginRateLimiter.Admission(false, 12));

        assertThatThrownBy(() -> service.authenticate("alice", "password", "10.0.0.1"))
                .isInstanceOf(LoginRateLimiter.ThrottledException.class);

        verifyNoInteractions(authenticationManager, users);
    }

    @Test
    void unexpectedAuthenticationFailureIsNotConvertedToCredentialFailure() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new AuthenticationServiceException("provider unavailable"));

        assertThatThrownBy(() -> service.authenticate("alice", "password", "10.0.0.1"))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    private static Authentication authentication(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    private static User user(boolean active) {
        User user = new User();
        user.setUsername("alice");
        user.setIsActive(active);
        return user;
    }
}
