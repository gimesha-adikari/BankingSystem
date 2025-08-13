package com.bankingsystem.core.dto;

import com.bankingsystem.core.enums.Gender;
import com.bankingsystem.core.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RoleUpdateRequest {
    @NotNull(message = "First name is required")
    private String firstName;
    @NotNull(message = "Last name is required")
    private String lastName;
    @NotNull(message = "Email is required")
    private String email;
    @NotNull(message = "Phone number is required")
    private String phone;
    @NotNull(message = "Address is required")
    private String address;
    @NotNull(message = "Role name is required")
    private String roleName;
    @NotNull(message = "Department is required")
    private String department;

    private UUID managerId;
    @NotNull(message = "Status is required")
    private Status status;
    @NotNull(message = "Gender is required")
    private Gender gender;
    @NotNull(message = "Date of Birth is required")
    private LocalDate dateOfBirth;
}
