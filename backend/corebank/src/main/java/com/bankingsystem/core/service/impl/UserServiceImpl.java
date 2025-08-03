package com.bankingsystem.core.service.impl;

import com.bankingsystem.core.dto.UpdateProfileRequest;
import com.bankingsystem.core.dto.UserProfileResponse;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.exceptions.BadRequestException;
import com.bankingsystem.core.exceptions.ConflictException;
import com.bankingsystem.core.exceptions.NotFoundException;
import com.bankingsystem.core.repository.UserRepository;
import com.bankingsystem.core.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailServiceImpl emailService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User Not Found with username: " + username);
                });
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        log.debug("Fetching current user profile");
        User user = getCurrentUser();

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRoleName(user.getRole().getRoleName());

        return response;
    }

    @Override
    @Transactional
    public void updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getPostalCode() != null) user.setPostalCode(request.getPostalCode());
        if (request.getHomeNumber() != null) user.setHomeNumber(request.getHomeNumber());
        if (request.getWorkNumber() != null) user.setWorkNumber(request.getWorkNumber());
        if (request.getOfficeNumber() != null) user.setOfficeNumber(request.getOfficeNumber());
        if (request.getMobileNumber() != null) user.setMobileNumber(request.getMobileNumber());

        userRepository.save(user);
        log.info("Profile updated successfully for user: {}", user.getUsername());
    }

    @Override
    public void requestEmailChange(String newEmail) {
        log.debug("Processing email change request to: {}", newEmail);
        User user = getCurrentUser();

        if (!newEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BadRequestException("Invalid email format");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Email is already in use");
        }

        String token = UUID.randomUUID().toString();

        user.setNewEmail(newEmail);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationTokenCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Email change request processed for user: {}. Verification email sent to: {}", user.getUsername(), newEmail);
        emailService.sendEmailChangeVerification(newEmail, token);
    }

    public void confirmEmailChange(String token) {
        log.debug("Processing email change confirmation with token");
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new NotFoundException("Invalid or expired token"));

        if (user.getEmailVerificationTokenCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new BadRequestException("Token expired. Please request a new verification email.");
        }

        if (user.getNewEmail() == null) {
            throw new BadRequestException("Email change already confirmed");
        }

        user.setEmail(user.getNewEmail());
        user.setNewEmail(null);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenCreatedAt(null);
        user.setEmailVerified(true);

        userRepository.save(user);
        log.info("Email change confirmed for user: {}", user.getUsername());
    }

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUser().getUserId();
    }


}
