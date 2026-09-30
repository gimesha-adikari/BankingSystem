package com.bankingsystem.core.features.accesscontrol.interfaces;

import com.bankingsystem.core.features.employee.interfaces.dto.EmployeeDto;
import com.bankingsystem.core.features.accesscontrol.domain.repository.RoleRepository;
import com.bankingsystem.core.features.accesscontrol.interfaces.dto.RoleUpdateRequest;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.employee.domain.Employee;
import com.bankingsystem.core.features.employee.domain.repository.EmployeeRepository;
import com.bankingsystem.core.features.accesscontrol.application.AccessControlService;
import com.bankingsystem.core.features.employee.application.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @GetMapping("/employees/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeDto>> getManagers() {
        List<Employee> managers = employeeRepository.findByRoleRoleNameIgnoreCase("MANAGER");
        List<EmployeeDto> employeeDtos = managers.stream()
                .map(employee -> new EmployeeDto(employee.getEmployeeId(), employee.getFirstName(), employee.getLastName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDtos);
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
