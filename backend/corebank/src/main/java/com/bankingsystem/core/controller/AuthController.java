package com.bankingsystem.core.controller;

import com.bankingsystem.core.dto.ChangePasswordRequest;
import com.bankingsystem.core.dto.JwtResponse;
import com.bankingsystem.core.dto.LoginRequest;
import com.bankingsystem.core.dto.RegisterRequest;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.repository.PasswordResetTokenRepository;
import com.bankingsystem.core.repository.UserRepository;
import com.bankingsystem.core.security.JwtUtils;
import com.bankingsystem.core.service.AuthService;
import com.bankingsystem.core.service.PasswordResetService;
import com.bankingsystem.core.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordResetService resetService;
    private final PasswordResetTokenRepository resetTokenRepository;


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
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!user.getIsActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Email not verified. Please verify your email first."));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String role = user.getRole().getRoleName();
            String jwt = jwtUtils.generateJwtToken(loginRequest.getUsername(), role);

            String ipAddress = request.getRemoteAddr();
            authService.createSession(jwt, loginRequest.getUsername(), ipAddress);

            return ResponseEntity.ok(new JwtResponse(jwt,user.getUsername(),role));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // catch all - for unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
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
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) {
        resetService.initiateReset(email);
        return ResponseEntity.ok("Reset email sent");
    }

    @GetMapping("/reset-password/{token}")
    public ResponseEntity<?> resetPassword(@PathVariable String token) {
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
    public ResponseEntity<?> resendVerification(@RequestBody Object request) {
        return ResponseEntity.ok("Resend verification email endpoint hit");
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

    @PostMapping("/auth/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> payload,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract the token from header "Bearer <token>"
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            String username = payload.get("username");

            // Validate the token (you may have a JwtUtils class or similar)
            if (!jwtUtils.validateJwtToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }

            // Optionally check if username from token matches username in body
            String tokenUsername = jwtUtils.getUserNameFromJwtToken(token);
            if (!tokenUsername.equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token username does not match payload username"));
            }

            // Generate a new token (usually extends expiry)
            String newToken = jwtUtils.generateJwtToken(username, userRepository.findByUsername(username).get().getRole().getRoleName());

            // Return the new token in the response
            return ResponseEntity.ok(Map.of("token", newToken));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Could not refresh token"));
        }
    }



}
