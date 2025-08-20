package com.bankingsystem.core.features.loans.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    @PostMapping("/apply")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> applyLoan(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Loan application submitted");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> listLoans() {
        return ResponseEntity.ok("List of all loans");
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyLoans() {
        return ResponseEntity.ok("User's loan details");
    }

    @PutMapping("/approve/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> approveLoan(@PathVariable String id) {
        return ResponseEntity.ok("Loan approved");
    }

    @PutMapping("/reject/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> rejectLoan(@PathVariable String id) {
        return ResponseEntity.ok("Loan rejected");
    }
}
