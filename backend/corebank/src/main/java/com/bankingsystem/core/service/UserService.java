package com.bankingsystem.core.service;

import com.bankingsystem.core.dto.UpdateProfileRequest;
import com.bankingsystem.core.dto.UserProfileResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();
    void updateProfile(UpdateProfileRequest request);
    void requestEmailChange(String newEmail);
    void confirmEmailChange(String token);
    UUID getCurrentUserId();
    List<UserProfileResponse> getAllUsers(String search);
    UserProfileResponse getUserById(UUID id);
    void deleteUser(UUID id);
}

