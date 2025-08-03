package com.bankingsystem.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RoleUpdateRequest {
    @NotNull(message = "Role name is required")
    private String roleName;
    private String department;
    private String firstName;
    private String lastName;
    private String email;
    @NotNull(message = "Phone number is required")
    private String phone;
    private UUID managerId;
}
