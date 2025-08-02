package com.bankingsystem.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scheduled-transactions")
public class ScheduledTransactionController {

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> scheduleTransaction(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Transaction scheduled");
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyScheduledTransactions() {
        return ResponseEntity.ok("Scheduled transactions");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelScheduledTransaction(@PathVariable String id) {
        return ResponseEntity.ok("Transaction cancelled");
    }
}
