package com.bankingsystem.core.features.accesscontrol.application.impl;

import com.bankingsystem.core.features.accesscontrol.domain.Role;
import com.bankingsystem.core.features.accesscontrol.interfaces.dto.RoleUpdateRequest;
import com.bankingsystem.core.modules.common.exceptions.EntityNotFoundException;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.features.employee.application.EmployeeService;
import com.bankingsystem.core.features.employee.domain.Employee;
import lombok.extern.slf4j.Slf4j;
import com.bankingsystem.core.features.employee.domain.repository.EmployeeRepository;
import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.accesscontrol.application.AccessControlService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        Role newRole = roleRepository.findByRoleNameIgnoreCase(request.getRoleName())
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        user.setRole(newRole);

        if (!"CUSTOMER".equalsIgnoreCase(request.getRoleName())) {
            Employee employee = employeeRepository.findByEmail(user.getEmail())
                    .orElseGet(Employee::new);

            employee.setEmail(request.getEmail());
            employee.setFirstName(request.getFirstName());
            employee.setLastName(request.getLastName());
            employee.setPhone(request.getPhone());
            employee.setDepartment(request.getDepartment());
            employee.setRole(newRole);

            if (request.getManagerId() != null) {
                Employee manager = employeeRepository.findById(request.getManagerId())
                        .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
                employee.setManager(manager);
            }

            if (employee.getHireDate() == null) {
                employee.setHireDate(LocalDateTime.now());
            }

            employee.setStatus(request.getStatus());
            employee.setGender(request.getGender());
            employee.setDateOfBirth(request.getDateOfBirth());
            employee.setAddress(request.getAddress());

            employee.setUser(user);
            user.setEmployee(employee);
            employeeRepository.save(employee);
            userRepository.save(user);
        } else {
            if (user.getEmployee() != null) {
                employeeService.resignEmployee(user.getUserId());
            }
        }
    }



}
