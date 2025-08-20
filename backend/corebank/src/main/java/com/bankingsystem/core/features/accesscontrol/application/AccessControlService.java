package com.bankingsystem.core.features.accesscontrol.application;

import com.bankingsystem.core.features.accesscontrol.interfaces.dto.RoleUpdateRequest;

import java.util.UUID;

public interface AccessControlService {
    void updateUserRole(UUID userId, RoleUpdateRequest request);
}
