package com.bankingsystem.core.features.customer.interfaces;

import com.bankingsystem.core.features.customer.application.CustomerService;
import com.bankingsystem.core.features.customer.interfaces.dto.CustomerRequestDTO;
import com.bankingsystem.core.features.customer.interfaces.dto.CustomerResponseDTO;
import com.bankingsystem.core.features.kyc.application.KycCaseService;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CurrentUserService currentUserService;
    private final KycCaseService kycCaseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER','MANAGER')")
    public ResponseEntity<List<CustomerResponseDTO>> listCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER','MANAGER')")
    public ResponseEntity<CustomerResponseDTO> createCustomer(@Valid @RequestBody CustomerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER','MANAGER')")
    public ResponseEntity<CustomerResponseDTO> updateCustomer(@Valid @RequestBody CustomerRequestDTO request) {
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

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerResponseDTO> getMe(Authentication auth) {
        UUID userId = currentUserService.requireUserId(auth);
        return ResponseEntity.ok(customerService.getByUserId(userId));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerResponseDTO> upsertMe(@Valid @RequestBody CustomerRequestDTO request,
                                                        Authentication auth) {
        UUID userId = currentUserService.requireUserId(auth);

        var latest = kycCaseService.getMyLatest(userId);
        if (latest.isEmpty() || latest.get().getStatus() != KycStatus.APPROVED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        request.setUserId(userId);

        CustomerResponseDTO dto = customerService.upsertByUser(request);
        return ResponseEntity.ok(dto);
    }
}
