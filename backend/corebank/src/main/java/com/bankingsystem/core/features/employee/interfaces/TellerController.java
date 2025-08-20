package com.bankingsystem.core.features.employee.interfaces;

import com.bankingsystem.core.features.employee.interfaces.dto.TellerRequest;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.employee.application.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tellers")
@RequiredArgsConstructor
public class TellerController {

    private final EmployeeService employeeService;
    private final UserRepository userRepository;


    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> listTellers() {
        return ResponseEntity.ok(employeeService.findEmployeesByRoleName("TELLER"));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> listAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> createTeller(@RequestBody TellerRequest request, @RequestParam UUID userId) {
        employeeService.createOrUpdateTeller(request, userId);
        return ResponseEntity.status(201).body("Teller assigned successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateTeller(@PathVariable String id, @RequestBody Object request) {
        employeeService.createOrUpdateTeller((TellerRequest) request, UUID.fromString(id));
        return ResponseEntity.ok("Teller updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteTeller(@PathVariable UUID id) {
        employeeService.resignEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
