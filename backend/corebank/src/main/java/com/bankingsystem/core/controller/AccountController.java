package com.bankingsystem.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public ResponseEntity<?> listAccounts() {
        return ResponseEntity.ok("List accounts");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','TELLER')")
    public ResponseEntity<?> openAccount(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Account opened");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER') or @securityService.isAccountOwner(authentication, #id)")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        return ResponseEntity.ok("Account details");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public ResponseEntity<?> updateAccount(@PathVariable String id, @RequestBody Object request) {
        return ResponseEntity.ok("Account updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public ResponseEntity<?> closeAccount(@PathVariable String id) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyAccounts() {
        return ResponseEntity.ok("User's accounts");
    }
}
