package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoginAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final LoginRateLimiter limiter;
    private final LoginRateLimitIdentityResolver identityResolver;

    public LoginAuthenticationService(@Qualifier("loginAuthenticationManager") AuthenticationManager authenticationManager,
                                      UserRepository users,
                                      LoginRateLimiter limiter,
                                      LoginRateLimitIdentityResolver identityResolver) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.limiter = limiter;
        this.identityResolver = identityResolver;
    }

    public LoginResult authenticate(String username, String password, String remoteAddress) {
        SecurityContextHolder.clearContext();
        LoginRateLimitIdentity identity;
        try {
            identity = identityResolver.resolve(username);
        } catch (LoginRateLimitIdentityResolver.IdentityResolutionException e) {
            log.error("AUTH_LOGIN_IDENTITY_FAILURE");
            throw new LoginRateLimiter.LimiterFailureException();
        }
        LoginRateLimiter.Admission admission;
        try {
            admission = limiter.tryAdmit(remoteAddress, identity);
        } catch (LoginRateLimiter.LimiterFailureException e) {
            log.error("AUTH_LOGIN_LIMITER_FAILURE");
            throw e;
        }
        if (!admission.allowed()) {
            log.info("AUTH_LOGIN_THROTTLED scope=LAYERED retryAfterSeconds={}", admission.retryAfterSeconds());
            throw new LoginRateLimiter.ThrottledException(admission.retryAfterSeconds());
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            if (!identity.isAccountIdentity()) {
                throw new LoginRateLimiter.LimiterFailureException();
            }
            User user = users.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            if (!identity.accountId().equals(user.getUserId())) {
                throw new IllegalStateException("Authenticated identity changed");
            }
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new DisabledException("Authenticated user is inactive");
            }

            try {
                limiter.resetAfterSuccessfulLogin(remoteAddress, identity);
            } catch (LoginRateLimiter.LimiterFailureException e) {
                log.error("AUTH_LOGIN_LIMITER_FAILURE");
                throw e;
            }
            log.info("AUTH_LOGIN_SUCCEEDED");
            return new LoginResult(authentication, user);
        } catch (DisabledException e) {
            log.info("AUTH_LOGIN_FAILED reason=ACCOUNT_INACTIVE");
            throw e;
        } catch (BadCredentialsException | UsernameNotFoundException | LockedException e) {
            log.info("AUTH_LOGIN_FAILED reason=INVALID_CREDENTIALS");
            throw e;
        }
    }

    public record LoginResult(Authentication authentication, User user) {
    }
}
