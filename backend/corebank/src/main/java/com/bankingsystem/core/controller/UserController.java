package com.bankingsystem.core.controller;

import com.bankingsystem.core.dto.*;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.repository.UserRepository;
import com.bankingsystem.core.service.AuthService;
import com.bankingsystem.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER','MANAGER')")
    public ResponseEntity<List<UserProfileResponse>> listAllUsers() {
        List<UserProfileResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestBody PasswordVerifyRequest request, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (matches) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(401).body("Invalid password");
        }
    }

    @PutMapping("/me/email")
    public ResponseEntity<?> changeEmail(@Valid @RequestBody EmailChangeRequest request) {
        userService.requestEmailChange(request.getNewEmail());
        return ResponseEntity.ok("Verification email sent");
    }

    @GetMapping("/me/email/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        userService.confirmEmailChange(token);
        return ResponseEntity.ok("Email updated successfully");
    }

    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileResponse user = userService.getCurrentUserProfile();
        return user.getUserId();
    }


}