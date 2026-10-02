package com.bankingsystem.core.modules.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAuthenticationProviderTest {

    @Test
    void unknownUsersUseProviderDummyPasswordMitigation() {
        UserDetailsService users = mock(UserDetailsService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);

        when(users.loadUserByUsername("missing")).thenThrow(new UsernameNotFoundException("missing"));
        when(encoder.encode(anyString())).thenReturn("dummy-hash");
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("missing", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(encoder).matches(eq("wrong"), anyString());
    }

    @Test
    void configuredProviderVerifiesInactivePasswordBeforePostCredentialRejection() {
        UserDetailsServiceImpl users = mock(UserDetailsServiceImpl.class);
        SecurityConfig securityConfig = new SecurityConfig(users, mock(JwtAuthFilter.class));
        AuthenticationProvider provider = securityConfig.authenticationProvider();
        String encodedPassword = securityConfig.passwordEncoder().encode("password");
        UserDetails inactive = User.withUsername("alice")
                .password(encodedPassword)
                .roles("CUSTOMER")
                .disabled(true)
                .build();
        when(users.loadUserByUsername("alice")).thenReturn(inactive);

        Authentication authentication = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("alice", "password"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("alice");
    }
}
