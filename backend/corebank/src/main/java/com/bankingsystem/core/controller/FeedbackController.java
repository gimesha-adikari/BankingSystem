package com.bankingsystem.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER')")
    public ResponseEntity<?> submitFeedback(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Feedback submitted");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> viewAllFeedback() {
        return ResponseEntity.ok("All feedback");
    }
}
