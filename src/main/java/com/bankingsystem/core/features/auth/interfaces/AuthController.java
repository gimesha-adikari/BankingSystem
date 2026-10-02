package com.bankingsystem.core.features.auth.interfaces;

import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.auth.interfaces.dto.*;
import com.bankingsystem.core.features.auth.domain.repository.PasswordResetTokenRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.auth.application.LoginAuthenticationService;
import com.bankingsystem.core.features.auth.application.LoginRateLimiter;
import com.bankingsystem.core.modules.common.security.JwtUtils;
import com.bankingsystem.core.features.auth.application.AuthService;
import com.bankingsystem.core.features.auth.application.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginAuthenticationService loginAuthenticationService;
    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordResetService resetService;
    private final PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    public AuthController(LoginAuthenticationService loginAuthenticationService,
                          JwtUtils jwtUtils,
                          AuthService authService,
                          UserRepository userRepository,
                          PasswordResetService resetService,
                          PasswordResetTokenRepository resetTokenRepository) {
        this.loginAuthenticationService = loginAuthenticationService;
        this.jwtUtils = jwtUtils;
        this.authService = authService;
        this.userRepository = userRepository;
        this.resetService = resetService;
        this.resetTokenRepository = resetTokenRepository;
    }

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          AuthService authService,
                          UserRepository userRepository,
                          PasswordResetService resetService,
                          PasswordResetTokenRepository resetTokenRepository) {
        this(new LoginAuthenticationService(
                        authenticationManager,
                        userRepository,
                        new LoginRateLimiter(new com.bankingsystem.core.modules.common.config.LoginRateLimitProperties())),
                jwtUtils,
                authService,
                userRepository,
                resetService,
                resetTokenRepository);
    }


    @GetMapping("/available")
    public ResponseEntity<?> isUsernameAvailable(@RequestParam String username) {
        log.info("Checking username nullability for: {}", username);
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username is required");
        }

        boolean exists = userRepository.existsByUsernameIgnoreCase(username);
        log.info("Checking username availability for: {}", username);
        log.info("Username availability: {}", exists);
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
        }

        return ResponseEntity.ok("Username available");
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        ResponseEntity<?> validation = validateLoginRequest(loginRequest);
        if (validation != null) {
            return validation;
        }
        try {
            LoginAuthenticationService.LoginResult result = loginAuthenticationService.authenticate(
                    loginRequest.getUsername(), loginRequest.getPassword(), request.getRemoteAddr());
            User user = result.user();
            String role = user.getRole().getRoleName();
            SecurityContextHolder.getContext().setAuthentication(result.authentication());
            String jwt = jwtUtils.generateJwtToken(loginRequest.getUsername(), role);
            authService.createSession(jwt, loginRequest.getUsername(), request.getRemoteAddr());

            return ResponseEntity.ok(new JwtResponse(jwt,user.getUsername(),role));
        } catch (LoginRateLimiter.ThrottledException e) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "no-store")
                    .header("Retry-After", Long.toString(e.getRetryAfterSeconds()))
                    .body(Map.of("error", "Too many login attempts. Try again later."));
        } catch (LoginRateLimiter.LimiterFailureException e) {
            SecurityContextHolder.clearContext();
            log.error("AUTH_LOGIN_LIMITER_FAILURE");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Service temporarily unavailable"));
        } catch (BadCredentialsException | UsernameNotFoundException | DisabledException | LockedException e) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "no-store")
                    .body(Map.of("error", "Invalid username or password"));
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            log.error("AUTH_LOGIN_INTERNAL_FAILURE type={}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    private ResponseEntity<?> validateLoginRequest(LoginRequest loginRequest) {
        Map<String, String> errors = new HashMap<>();
        if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getUsername().isBlank()) {
            errors.put("username", "must not be blank");
        }
        if (loginRequest == null || loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
            errors.put("password", "must not be blank");
        }
        if (errors.isEmpty()) {
            return null;
        }
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("code", "ERR_VALIDATION", "message", "Validation failed", "errors", errors));
    }


    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = jwtUtils.resolveToken(request);
        if (token == null) {
            return ResponseEntity.badRequest().body("Invalid token");
        }
        try {
            authService.logout(token);
            return ResponseEntity.ok("Logged out successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        resetService.initiateReset(req.getEmail());
        return ResponseEntity.ok("If an account exists, a reset email has been sent");
    }


    @GetMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam("token") String token) {
        try {
            resetTokenRepository.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        }catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok("Valid Reset Token!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        resetService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password has been reset");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        boolean isVerified = authService.verifyEmail(token);
        if (isVerified) {
            return ResponseEntity.ok("Email verified successfully");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired token");
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok("If an account exists, a verification email has been sent");
    }

    @GetMapping("/validate-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");

        if (!jwtUtils.validateJwtToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        String username = jwtUtils.getUserNameFromJwtToken(token);
        String role = jwtUtils.getRoleFromJwtToken(token);

        Map<String, String> response = new HashMap<>();
        response.put("username", username);
        response.put("role", role);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> payload,
                                          @RequestHeader("Authorization") String authHeader,
                                          HttpServletRequest request) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            String username = payload.get("username");

            if (!jwtUtils.validateJwtToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            if (!authService.isSessionValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid session"));
            }

            String tokenUsername = jwtUtils.getUserNameFromJwtToken(token);
            if (!tokenUsername.equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token username does not match payload username"));
            }

            String role = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                    .getRole().getRoleName();
            String newToken = jwtUtils.generateJwtToken(username, role);

            try {
                authService.logout(token);
            } catch (RuntimeException ignored) {
                // Old session might already be inactive
            }

            String ipAddress = request != null ? request.getRemoteAddr() : "127.0.0.1";
            authService.createSession(newToken, username, ipAddress);

            return ResponseEntity.ok(Map.of("token", newToken));
        } catch (Exception e) {
            log.error("Could not refresh token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Could not refresh token"));
        }
    }



}
