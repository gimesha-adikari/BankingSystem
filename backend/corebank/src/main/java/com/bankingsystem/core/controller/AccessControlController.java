package com.bankingsystem.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/access-control")
public class AccessControlController {

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listRoles() {
        return ResponseEntity.ok("List of roles");
    }

    @PutMapping("/roles/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRole(@PathVariable String userId, @RequestBody Object request) {
        return ResponseEntity.ok("User role updated");
    }
}
