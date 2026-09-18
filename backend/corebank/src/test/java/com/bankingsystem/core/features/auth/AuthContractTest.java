package com.bankingsystem.core.features.auth;

import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.domain.VerificationToken;
import com.bankingsystem.core.features.auth.domain.repository.SessionRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.auth.domain.repository.VerificationTokenRepository;
import com.bankingsystem.core.features.auth.application.impl.AuthServiceImpl;
import com.bankingsystem.core.features.auth.interfaces.dto.ForgotPasswordRequest;
import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.system.application.EmailService;
import com.bankingsystem.core.modules.common.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthContractTest {

    @Test
    void forgotPasswordRequestUsesJsonEmailField() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ForgotPasswordRequest request = mapper.readValue(
                "{\"email\":\"customer@example.test\"}",
                ForgotPasswordRequest.class
        );

        assertThat(request.getEmail()).isEqualTo("customer@example.test");
        assertThat(mapper.writeValueAsString(request)).contains("\"email\":\"customer@example.test\"");
    }

    @Test
    void resendVerificationReusesTheExistingEmailFlowForUnverifiedUsers() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        SessionRepository sessions = mock(SessionRepository.class);
        VerificationTokenRepository tokens = mock(VerificationTokenRepository.class);
        EmailService email = mock(EmailService.class);
        AppProperties properties = new AppProperties();
        AppProperties.Token tokenProperties = new AppProperties.Token();
        tokenProperties.setExpirationHours(24);
        properties.setToken(tokenProperties);

        AuthServiceImpl service = new AuthServiceImpl(users, roles, encoder, sessions, tokens, email, properties);
        User user = new User();
        user.setEmail("customer@example.test");
        user.setIsActive(false);
        user.setEmailVerified(false);
        when(users.findByEmail("customer@example.test")).thenReturn(Optional.of(user));
        when(tokens.findByUser(user)).thenReturn(Optional.empty());

        service.resendVerification(" customer@example.test ");

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokens).save(captor.capture());
        verify(email).sendVerificationEmail(eq("customer@example.test"), anyString());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getExpiryDate()).isNotNull();
    }

    @Test
    void resendVerificationDoesNotRevealWhetherAnUnknownEmailExists() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        SessionRepository sessions = mock(SessionRepository.class);
        VerificationTokenRepository tokens = mock(VerificationTokenRepository.class);
        EmailService email = mock(EmailService.class);
        AppProperties properties = new AppProperties();
        AuthServiceImpl service = new AuthServiceImpl(users, roles, encoder, sessions, tokens, email, properties);
        when(users.findByEmail("nobody@example.test")).thenReturn(Optional.empty());

        service.resendVerification("nobody@example.test");

        verifyNoInteractions(tokens, email);
    }
}
