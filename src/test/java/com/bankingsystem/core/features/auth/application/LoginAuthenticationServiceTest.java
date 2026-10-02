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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoginAuthenticationServiceTest {

    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    UserRepository users = mock(UserRepository.class);
    LoginRateLimiter limiter = mock(LoginRateLimiter.class);
    LoginRateLimitIdentityResolver identityResolver = mock(LoginRateLimitIdentityResolver.class);
    LoginAuthenticationService service = new LoginAuthenticationService(authenticationManager, users, limiter, identityResolver);
    LoginRateLimitIdentity accountIdentity = LoginRateLimitIdentity.forAccount(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        when(identityResolver.resolve(anyString())).thenReturn(accountIdentity);
        when(limiter.tryAdmit(anyString(), any(LoginRateLimitIdentity.class))).thenReturn(LoginRateLimiter.Admission.permitted());
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

        verify(users, never()).findByUsername(anyString());
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
        verify(limiter, never()).resetAfterSuccessfulLogin(anyString(), any(LoginRateLimitIdentity.class));
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
        verify(identityResolver).resolve(" alice ");
        verify(limiter).tryAdmit("10.0.0.1", accountIdentity);
        verify(limiter).resetAfterSuccessfulLogin("10.0.0.1", accountIdentity);
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
        when(limiter.tryAdmit(anyString(), any(LoginRateLimitIdentity.class)))
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

    @Test
    void unknownIdentityStillInvokesAuthentication() {
        LoginRateLimitIdentity unknown = LoginRateLimitIdentity.forUnknown("missing");
        when(identityResolver.resolve("missing")).thenReturn(unknown);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> service.authenticate("missing", "wrong", "10.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(users, never()).findByUsername(anyString());
    }

    @Test
    void identityResolutionFailureStopsAuthenticationAndDoesNotFallback() {
        when(identityResolver.resolve("alice"))
                .thenThrow(new LoginRateLimitIdentityResolver.IdentityResolutionException());

        assertThatThrownBy(() -> service.authenticate("alice", "password", "10.0.0.1"))
                .isInstanceOf(LoginRateLimiter.LimiterFailureException.class);

        verifyNoInteractions(authenticationManager, limiter);
    }

    @Test
    void accountIdentityIsRequiredBeforeSuccessfulReset() {
        when(identityResolver.resolve("alice")).thenReturn(LoginRateLimitIdentity.forUnknown("alice"));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication("alice"));

        assertThatThrownBy(() -> service.authenticate("alice", "password", "10.0.0.1"))
                .isInstanceOf(LoginRateLimiter.LimiterFailureException.class);

        verifyNoInteractions(users);
        verify(limiter, never()).resetAfterSuccessfulLogin(anyString(), any(LoginRateLimitIdentity.class));
    }

    @Test
    void authenticatedIdentityDisappearanceRemainsAnInternalFailure() {
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication("alice"));
        when(users.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate("alice", "password", "10.0.0.1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void authenticatedUserMustMatchResolvedAccountIdentity() {
        User resolvedUser = user(true);
        when(users.findById(accountIdentity.accountId())).thenReturn(Optional.of(resolvedUser));
        User differentUser = user(true);
        differentUser.setUserId(UUID.randomUUID());
        when(users.findByUsername("alice")).thenReturn(Optional.of(differentUser));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication("alice"));

        assertThatThrownBy(() -> service.authenticate("alice", "password", "10.0.0.1"))
                .isInstanceOf(IllegalStateException.class);

        verify(limiter, never()).resetAfterSuccessfulLogin(anyString(), any(LoginRateLimitIdentity.class));
    }

    private static Authentication authentication(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    private User user(boolean active) {
        User user = new User();
        user.setUsername("alice");
        user.setIsActive(active);
        user.setUserId(accountIdentity.accountId());
        return user;
    }
}
