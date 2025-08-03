package com.bankingsystem.core.service;

import com.bankingsystem.core.dto.TellerRequest;
import com.bankingsystem.core.entity.Employee;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {
    void createOrUpdateTeller(TellerRequest request, UUID userId);
    void resignEmployee(UUID userId);
    List<Employee> findEmployeesByRoleName(String roleName);
}
