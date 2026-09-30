package com.bankingsystem.core.modules.common.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void accessDeniedExceptionMapsTo403() {
        ResponseEntity<?> response = handler.handleAccessDenied(new AccessDeniedException("Access denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo("ERR_FORBIDDEN");
        assertThat(body.get("message")).isEqualTo("Access denied");
    }

    @Test
    void authorizationDeniedExceptionMapsTo403() {
        ResponseEntity<?> response = handler.handleAuthorizationDenied(
                new AuthorizationDeniedException("Authorization denied", () -> false)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo("ERR_FORBIDDEN");
    }
}
