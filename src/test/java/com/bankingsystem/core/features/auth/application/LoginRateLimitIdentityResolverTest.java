package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoginRateLimitIdentityResolverTest {

    private final UserRepository users = mock(UserRepository.class);
    private final LoginRateLimitIdentityResolver resolver = new LoginRateLimitIdentityResolver(users);

    @Test
    void databaseEquivalentAliasesProduceTheSameAccountIdentity() {
        UUID userId = UUID.randomUUID();
        when(users.findUserIdByUsername("jose")).thenReturn(Optional.of(userId));
        when(users.findUserIdByUsername("josé")).thenReturn(Optional.of(userId));

        LoginRateLimitIdentity jose = resolver.resolve("jose");
        LoginRateLimitIdentity joseWithAccent = resolver.resolve("josé");

        assertThat(jose).isEqualTo(joseWithAccent);
        assertThat(jose.isAccountIdentity()).isTrue();
        assertThat(jose.key()).isEqualTo("account:" + userId);
        verify(users).findUserIdByUsername("jose");
        verify(users).findUserIdByUsername("josé");
        verify(users, never()).findByUsername(anyString());
    }

    @Test
    void emptyDatabaseResultUsesSeparateUnknownFallbackIdentity() {
        when(users.findUserIdByUsername("  Missing  ")).thenReturn(Optional.empty());

        LoginRateLimitIdentity identity = resolver.resolve("  Missing  ");

        assertThat(identity.isAccountIdentity()).isFalse();
        assertThat(identity.key()).isEqualTo("unknown:missing");
    }

    @Test
    void databaseFailureDoesNotBecomeUnknownIdentity() {
        when(users.findUserIdByUsername("alice")).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> resolver.resolve("alice"))
                .isInstanceOf(LoginRateLimitIdentityResolver.IdentityResolutionException.class);
    }
}
