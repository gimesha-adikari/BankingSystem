package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoginAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final LoginRateLimiter limiter;

    public LoginAuthenticationService(AuthenticationManager authenticationManager,
                                      UserRepository users,
                                      LoginRateLimiter limiter) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.limiter = limiter;
    }

    public LoginResult authenticate(String username, String password, String remoteAddress) {
        SecurityContextHolder.clearContext();
        LoginRateLimiter.Admission admission;
        try {
            admission = limiter.tryAdmit(remoteAddress, username);
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
            User user = users.findByUsername(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new DisabledException("Authenticated user is inactive");
            }

            try {
                limiter.resetAfterSuccessfulLogin(remoteAddress, username);
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
