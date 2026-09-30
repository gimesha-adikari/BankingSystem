package com.bankingsystem.core.modules.common.security;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Component; import java.util.UUID;
@Component @RequiredArgsConstructor public class AuthenticatedUserIdProvider { private final UserRepository users; public UUID userId(Authentication a){ if(a==null||!a.isAuthenticated()) throw new IllegalStateException("Authenticated principal required"); return users.findByUsername(a.getName()).map(u->u.getUserId()).orElseThrow(()->new IllegalStateException("Authenticated user not found")); } }
