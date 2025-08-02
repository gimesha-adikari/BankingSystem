package com.bankingsystem.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('CUSTOMER','TELLER','ADMIN')")
    public ResponseEntity<?> deposit(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Deposit successful");
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('CUSTOMER','TELLER','ADMIN')")
    public ResponseEntity<?> withdraw(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Withdrawal successful");
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('CUSTOMER','TELLER','ADMIN')")
    public ResponseEntity<?> transfer(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Transfer successful");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public ResponseEntity<?> listTransactions() {
        return ResponseEntity.ok("All transactions");
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyTransactions() {
        return ResponseEntity.ok("User's transactions");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER') or @securityService.isTransactionOwner(authentication, #id)")
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok("Transaction details");
    }
}
