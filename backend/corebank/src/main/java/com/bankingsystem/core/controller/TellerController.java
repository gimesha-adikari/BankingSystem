package com.bankingsystem.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tellers")
public class TellerController {

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> listTellers() {
        return ResponseEntity.ok("List of tellers");
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> createTeller(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Teller created");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateTeller(@PathVariable String id, @RequestBody Object request) {
        return ResponseEntity.ok("Teller updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteTeller(@PathVariable String id) {
        return ResponseEntity.noContent().build();
    }
}
