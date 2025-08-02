package com.bankingsystem.core.controller;

import com.bankingsystem.core.dto.ChangePasswordRequest;
import com.bankingsystem.core.dto.EmailChangeRequest;
import com.bankingsystem.core.dto.UpdateProfileRequest;
import com.bankingsystem.core.dto.UserProfileResponse;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.service.AuthService;
import com.bankingsystem.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

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

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok("Password updated successfully");
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