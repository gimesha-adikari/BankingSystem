// com/bankingsystem/core/security/CurrentUserService.java
package com.bankingsystem.core.modules.common.security;

import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository users;

    public UUID requireUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        String username = auth.getName();
        return users.findByUsername(username)
                .map(User::getUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    public String requireUsername(Authentication auth) {
        if (auth == null || auth.getName() == null) throw new IllegalStateException("Unauthenticated");
        return auth.getName();
    }
}
