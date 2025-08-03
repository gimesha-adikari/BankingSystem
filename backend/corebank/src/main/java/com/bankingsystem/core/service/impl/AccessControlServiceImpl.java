package com.bankingsystem.core.service.impl;

import com.bankingsystem.core.dto.RoleUpdateRequest;
import com.bankingsystem.core.exceptions.EntityNotFoundException;
import com.bankingsystem.core.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import com.bankingsystem.core.entity.*;
import com.bankingsystem.core.repository.EmployeeRepository;
import com.bankingsystem.core.repository.RoleRepository;
import com.bankingsystem.core.repository.UserRepository;
import com.bankingsystem.core.service.AccessControlService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlServiceImpl implements AccessControlService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    @Override
    @Transactional
    public void updateUserRole(UUID userId, RoleUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Role newRole = roleRepository.findByRoleName(request.getRoleName().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        user.setRole(newRole);
        log.info("Updated role for user {} to {}", userId, request.getRoleName());

        if (!"CUSTOMER".equalsIgnoreCase(request.getRoleName())) {
            Optional<Employee> existingEmployee = employeeRepository.findByEmail(user.getEmail());
            Employee employee = existingEmployee.orElseGet(Employee::new);

            employee.setEmail(request.getEmail() != null ? request.getEmail() : user.getEmail());

            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                employee.setFirstName(request.getFirstName());
            } else {
                throw new IllegalArgumentException("First name is required for employee");
            }

            if (request.getLastName() != null && !request.getLastName().isBlank()) {
                employee.setLastName(request.getLastName());
            } else {
                throw new IllegalArgumentException("Last name is required for employee");
            }

            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                employee.setPhone(request.getPhone());
            }

            if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
                employee.setDepartment(request.getDepartment());
            }

            employee.setRole(newRole);

            if (request.getManagerId() != null) {
                Employee manager = employeeRepository.findById(request.getManagerId())
                        .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
                employee.setManager(manager);
            }

            if (existingEmployee.isEmpty()) {
                employee.setHireDate(LocalDateTime.now());
            }

            employee.setStatus(Employee.Status.ACTIVE);
            employee.setUser(user);
            user.setEmployee(employee);
            userRepository.save(user);
            employeeRepository.save(employee);
        } else {
            if (user.getEmployee() != null) {
                employeeService.resignEmployee(user.getUserId());
            }
        }
    }


}
