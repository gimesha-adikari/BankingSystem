package com.bankingsystem.core.controller;

import com.bankingsystem.core.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {
    private final BranchRepository branchRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TELLER','CUSTOMER')")
    public ResponseEntity<?> listBranches(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(branchRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBranch(@RequestBody Object request) {
        return ResponseEntity.status(201).body("Branch created");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TELLER','CUSTOMER')")
    public ResponseEntity<?> getBranchById(@PathVariable Integer id) {
        return ResponseEntity.ok(branchRepository.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBranch(@PathVariable String id, @RequestBody Object request) {
        return ResponseEntity.ok("Branch updated for id: " + id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBranch(@PathVariable String id) {
        return ResponseEntity.noContent().build();
    }
}
