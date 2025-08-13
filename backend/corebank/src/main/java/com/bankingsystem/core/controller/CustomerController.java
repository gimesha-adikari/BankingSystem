package com.bankingsystem.core.controller;

import com.bankingsystem.core.dto.CustomerRequestDTO;
import com.bankingsystem.core.dto.CustomerResponseDTO;
import com.bankingsystem.core.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER','MANAGER')")
    public ResponseEntity<List<CustomerResponseDTO>> listCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','MANAGER')")
    public ResponseEntity<CustomerResponseDTO> createCustomer(@RequestBody CustomerRequestDTO request) {
        log.info("Creating customer {}", request);
        return ResponseEntity.status(201).body(customerService.createCustomer(request));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER','MANAGER')")
    public ResponseEntity<CustomerResponseDTO> updateCustomer( @RequestBody CustomerRequestDTO request) {
        return ResponseEntity.ok(customerService.updateCustomer(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER','MANAGER')")
    public ResponseEntity<CustomerResponseDTO> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
