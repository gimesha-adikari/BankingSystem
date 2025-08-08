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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailServiceImpl emailService;
    private final PasswordEncoder passwordEncoder;

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
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setAddress(user.getAddress());
        response.setCity(user.getCity());
        response.setState(user.getState());
        response.setCountry(user.getCountry());
        response.setPostalCode(user.getPostalCode());
        response.setHomeNumber(user.getHomeNumber());
        response.setWorkNumber(user.getWorkNumber());
        response.setOfficeNumber(user.getOfficeNumber());
        response.setMobileNumber(user.getMobileNumber());
        response.setRoleName(user.getRole().getRoleName());

        return response;
    }

    @Override
    @Transactional
    public void updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (userRepository.existsByUsername(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
                throw new ConflictException("Username is already taken");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getFirstName() != null && !request.getFirstName().isBlank())
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null && !request.getLastName().isBlank()) user.setLastName(request.getLastName());
        if (request.getAddress() != null && !request.getAddress().isBlank()) user.setAddress(request.getAddress());
        if (request.getCity() != null && !request.getCity().isBlank()) user.setCity(request.getCity());
        if (request.getState() != null && !request.getState().isBlank()) user.setState(request.getState());
        if (request.getCountry() != null && !request.getCountry().isBlank()) user.setCountry(request.getCountry());
        if (request.getPostalCode() != null && !request.getPostalCode().isBlank())
            user.setPostalCode(request.getPostalCode());
        if (request.getHomeNumber() != null && !request.getHomeNumber().isBlank())
            user.setHomeNumber(request.getHomeNumber());
        if (request.getWorkNumber() != null && !request.getWorkNumber().isBlank())
            user.setWorkNumber(request.getWorkNumber());
        if (request.getOfficeNumber() != null && !request.getOfficeNumber().isBlank())
            user.setOfficeNumber(request.getOfficeNumber());
        if (request.getMobileNumber() != null && !request.getMobileNumber().isBlank())
            user.setMobileNumber(request.getMobileNumber());
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            requestEmailChange(request.getEmail());
        }

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

    @Override
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> "CUSTOMER".equals(user.getRole().getRoleName()))
                .map(user -> {
                    UserProfileResponse dto = new UserProfileResponse();
                    dto.setUserId(user.getUserId());
                    dto.setUsername(user.getUsername());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setEmail(user.getEmail());
                    dto.setAddress(user.getAddress());
                    dto.setCity(user.getCity());
                    dto.setState(user.getState());
                    dto.setCountry(user.getCountry());
                    dto.setPostalCode(user.getPostalCode());
                    dto.setHomeNumber(user.getHomeNumber());
                    dto.setWorkNumber(user.getWorkNumber());
                    dto.setOfficeNumber(user.getOfficeNumber());
                    dto.setMobileNumber(user.getMobileNumber());
                    dto.setRoleName(user.getRole().getRoleName());
                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Override
    public UserProfileResponse getUserById(UUID id) {
        return null;
    }

    @Override
    public void deleteUser(UUID id) {

    }


}
