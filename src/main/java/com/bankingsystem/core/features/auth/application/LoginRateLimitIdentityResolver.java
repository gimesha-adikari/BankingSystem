package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginRateLimitIdentityResolver {

    private final UserRepository users;

    public LoginRateLimitIdentity resolve(String username) {
        try {
            return users.findUserIdByUsername(username)
                    .map(LoginRateLimitIdentity::forAccount)
                    .orElseGet(() -> LoginRateLimitIdentity.forUnknown(username));
        } catch (RuntimeException e) {
            throw new IdentityResolutionException(e);
        }
    }

    public static final class IdentityResolutionException extends RuntimeException {
        public IdentityResolutionException(Throwable cause) {
            super(cause);
        }

        public IdentityResolutionException() {
        }
    }
}
