package com.bankingsystem.core.service.impl;

import com.bankingsystem.core.dto.RoleUpdateRequest;
import com.bankingsystem.core.dto.TellerRequest;
import com.bankingsystem.core.entity.Employee;
import com.bankingsystem.core.entity.Role;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.exceptions.NotFoundException;
import com.bankingsystem.core.repository.EmployeeRepository;
import com.bankingsystem.core.repository.RoleRepository;
import com.bankingsystem.core.repository.UserRepository;
import com.bankingsystem.core.service.AccessControlService;
import com.bankingsystem.core.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        RoleUpdateRequest roleUpdateRequest = RoleUpdateRequest.builder()
                .roleName("TELLER")
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .department(request.getDepartment())
                .managerId(request.getManagerId())
                .build();
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

        if (employee.getStatus() == Employee.Status.INACTIVE) {
            throw new IllegalStateException("Employee already resigned.");
        }

        employee.setStatus(Employee.Status.INACTIVE);
        employee.setResignationDate(LocalDateTime.now());
        employeeRepository.save(employee);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new NotFoundException("Role not found"));
        user.setRole(customerRole);
        user.setEmployee(null);
        userRepository.save(user);
        log.info("Employee with ID {} resigned. Changed role from {} to CUSTOMER",
                userId, user.getRole().getRoleName());
    }

    @Override
    public List<Employee> findEmployeesByRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return employeeRepository.findAll();
        } else {
            Role role = roleRepository.findByRoleName(roleName.toUpperCase())
                    .orElseThrow(() -> new NotFoundException("Role not found"));
            return employeeRepository.findByRole(role);
        }
    }
}

