package com.bankingsystem.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class EmployeeResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String roleName;
    private String status;
    private String department;
    private String phone;
    private String address;
    private String gender;
    private String dateOfBirth;
    private LocalDateTime hireDate;
    private UUID managerId;
    private UUID userId;

}
