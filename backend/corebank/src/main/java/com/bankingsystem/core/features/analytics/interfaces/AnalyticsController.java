package com.bankingsystem.core.features.analytics.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    @GetMapping("/fraud-detection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> runFraudDetection() {
        return ResponseEntity.ok("Fraud detection results");
    }

    @GetMapping("/spending-patterns")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getSpendingPatterns() {
        return ResponseEntity.ok("Spending analytics");
    }
}
