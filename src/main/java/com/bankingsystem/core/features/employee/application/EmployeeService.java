package com.bankingsystem.core.features.employee.application;

import com.bankingsystem.core.features.employee.interfaces.dto.EmployeeResponse;
import com.bankingsystem.core.features.employee.interfaces.dto.TellerRequest;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {
    void createOrUpdateTeller(TellerRequest request, UUID userId);
    void resignEmployee(UUID userId);
    List<EmployeeResponse> findEmployeesByRoleName(String roleName);
}
