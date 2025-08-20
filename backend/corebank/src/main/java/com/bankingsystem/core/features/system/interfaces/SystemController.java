package com.bankingsystem.core.features.system.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSystemLogs() {
        return ResponseEntity.ok("System logs");
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSystemStatus() {
        return ResponseEntity.ok("System is healthy");
    }

    @PostMapping("/backup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBackup() {
        return ResponseEntity.ok("Backup created");
    }

    @PostMapping("/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restoreBackup() {
        return ResponseEntity.ok("System restored");
    }
}
