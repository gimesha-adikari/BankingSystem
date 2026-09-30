package com.bankingsystem.core.features.employee.application.impl;

import com.bankingsystem.core.features.employee.interfaces.dto.EmployeeResponse;
import com.bankingsystem.core.features.accesscontrol.interfaces.dto.RoleUpdateRequest;
import com.bankingsystem.core.features.employee.interfaces.dto.TellerRequest;
import com.bankingsystem.core.features.employee.domain.Employee;
import com.bankingsystem.core.features.accesscontrol.domain.Role;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.modules.common.enums.Status;
import com.bankingsystem.core.modules.common.exceptions.NotFoundException;
import com.bankingsystem.core.features.employee.domain.repository.EmployeeRepository;
import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.accesscontrol.application.AccessControlService;
import com.bankingsystem.core.features.employee.application.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccessControlService accessControlService;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               @Lazy AccessControlService accessControlService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accessControlService = accessControlService;
    }

    @Override
    public void createOrUpdateTeller(TellerRequest request, UUID userId) {
        RoleUpdateRequest roleUpdateRequest = new RoleUpdateRequest();
        roleUpdateRequest.setRoleName("TELLER");
        roleUpdateRequest.setFirstName(request.getFirstName());
        roleUpdateRequest.setLastName(request.getLastName());
        roleUpdateRequest.setPhone(request.getPhone());
        roleUpdateRequest.setDepartment(request.getDepartment());
        roleUpdateRequest.setManagerId(request.getManagerId());
        if (userRepository.findById(userId).get().getRole().getRoleName().equalsIgnoreCase("ADMIN")) {
            throw new IllegalStateException("Admin cannot create or update teller");
        }

        accessControlService.updateUserRole(userId, roleUpdateRequest);
        log.info("Teller with ID {} created/updated successfully", userId);
    }

    @Override
    @Transactional
    public void resignEmployee(UUID userId) {
        Employee employee = employeeRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        if (employee.getStatus() == Status.INACTIVE) {
            throw new IllegalStateException("Employee already resigned.");
        }

        employee.setStatus(Status.INACTIVE);
        employee.setResignationDate(LocalDateTime.now());
        employeeRepository.save(employee);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Role customerRole = roleRepository.findByRoleNameIgnoreCase("CUSTOMER")
                .orElseThrow(() -> new NotFoundException("Role not found"));
        user.setRole(customerRole);
        user.setEmployee(null);
        userRepository.save(user);
        log.info("Employee with ID {} resigned. Changed role from {} to CUSTOMER",
                userId, user.getRole().getRoleName());
    }
    @Override
    public List<EmployeeResponse> findEmployeesByRoleName(String roleName) {
        List<Employee> employees;
        if (roleName == null || roleName.isBlank()) {
            employees = employeeRepository.findAll();
        } else {
            Role role = roleRepository.findByRoleNameIgnoreCase(roleName.toUpperCase())
                    .orElseThrow(() -> new NotFoundException("Role not found"));
            employees = employeeRepository.findByRole(role);
        }

        return employees.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        EmployeeResponse resp = new EmployeeResponse();
        resp.setFirstName(employee.getFirstName());
        resp.setLastName(employee.getLastName());
        resp.setEmail(employee.getEmail());
        resp.setRoleName(employee.getRole() != null ? employee.getRole().getRoleName() : null);
        resp.setStatus(employee.getStatus() != null ? employee.getStatus().name() : null);
        resp.setDepartment(employee.getDepartment());
        resp.setPhone(employee.getPhone());
        resp.setAddress(employee.getAddress());
        resp.setGender(employee.getGender() != null ? employee.getGender().name() : null);
        resp.setDateOfBirth(employee.getDateOfBirth() != null ? employee.getDateOfBirth().toString() : null);
        resp.setHireDate(employee.getHireDate());
        resp.setManagerId(employee.getManager() != null ? employee.getManager().getEmployeeId() : null);
        resp.setUserId(employee.getUser().getUserId());
        return resp;
    }
}

