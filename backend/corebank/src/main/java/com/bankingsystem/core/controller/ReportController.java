package com.bankingsystem.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('MANAGER', 'TELLER')")
    public ResponseEntity<?> getDailyReport() {
        return ResponseEntity.ok("Daily report");
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> getMonthlyReport() {
        return ResponseEntity.ok("Monthly report");
    }

    @GetMapping("/customer/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'TELLER')")
    public ResponseEntity<?> getCustomerReport(@PathVariable String id) {
        return ResponseEntity.ok("Customer report");
    }
}
