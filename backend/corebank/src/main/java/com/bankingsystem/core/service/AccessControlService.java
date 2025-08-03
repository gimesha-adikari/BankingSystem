package com.bankingsystem.core.service;

import com.bankingsystem.core.dto.RoleUpdateRequest;

import java.util.UUID;

public interface AccessControlService {
    void updateUserRole(UUID userId, RoleUpdateRequest request);
}
