package com.bankingsystem.core.controller;

import com.bankingsystem.core.dto.ChangePasswordRequest;
import com.bankingsystem.core.dto.JwtResponse;
import com.bankingsystem.core.dto.LoginRequest;
import com.bankingsystem.core.dto.RegisterRequest;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.repository.UserRepository;
import com.bankingsystem.core.security.JwtUtils;
import com.bankingsystem.core.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        boolean isVerified = authService.verifyEmail(token);
        if (isVerified) {
            return ResponseEntity.ok("Email verified successfully");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired token");
        }
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
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getIsActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email not verified. Please verify your email first.");
            }

            String jwt = jwtUtils.generateJwtToken(loginRequest.getUsername());

            String ipAddress = request.getRemoteAddr();

            authService.createSession(jwt, loginRequest.getUsername(), ipAddress);


            return ResponseEntity.ok(new JwtResponse(jwt));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    @PostMapping("/logout")
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
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok("Password changed successfully");
    }


}
