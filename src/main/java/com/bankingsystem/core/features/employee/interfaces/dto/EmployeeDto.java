package com.bankingsystem.core.features.employee.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EmployeeDto {
    private UUID employeeId;
    private String firstName;
    private String lastName;

    public EmployeeDto(UUID employeeId, String firstName, String lastName) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}