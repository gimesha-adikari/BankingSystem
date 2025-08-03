package com.bankingsystem.core.controller;

import com.bankingsystem.core.dto.RoleUpdateRequest;
import com.bankingsystem.core.entity.*;
import com.bankingsystem.core.repository.*;
import com.bankingsystem.core.service.AccessControlService;
import com.bankingsystem.core.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access-control")
@RequiredArgsConstructor
public class AccessControlController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final AccessControlService accessControlService;
    private final EmployeeService employeeService;

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listAllEmployees() {
        return ResponseEntity.ok(employeeService.findEmployeesByRoleName(null));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/users/{userName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable String userName) {
        return ResponseEntity.ok(userRepository.findByUsername(userName));
    }


    @PutMapping("/roles/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable UUID userId,@Valid @RequestBody RoleUpdateRequest request) {
        accessControlService.updateUserRole(userId, request);
        return ResponseEntity.ok("User role and employee status updated successfully");
    }
}
