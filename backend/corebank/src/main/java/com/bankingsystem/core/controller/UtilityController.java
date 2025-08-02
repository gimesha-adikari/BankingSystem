package com.bankingsystem.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/utils")
public class UtilityController {

    @GetMapping("/roles")
    public ResponseEntity<?> getRolesEnum() {
        return ResponseEntity.ok("All roles enum");
    }

    @GetMapping("/account-types")
    public ResponseEntity<?> getAccountTypesEnum() {
        return ResponseEntity.ok("Account types enum");
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok("API is healthy");
    }
}
