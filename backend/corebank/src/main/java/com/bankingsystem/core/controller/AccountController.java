package com.bankingsystem.core.controller;

import com.bankingsystem.core.dto.AccountRequestDTO;
import com.bankingsystem.core.dto.AccountResponseDTO;
import com.bankingsystem.core.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','MANAGER')")
    public ResponseEntity<List<AccountResponseDTO>> listAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER'")
    public ResponseEntity<AccountResponseDTO> openAccount(@RequestBody AccountRequestDTO request) {
        return ResponseEntity.status(201).body(accountService.openAccount(request,null));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('TELLER')")
    public ResponseEntity<AccountResponseDTO> openAccountForCustomer(@RequestBody AccountRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.status(201).body(accountService.openAccount(request,id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','MANAGER') or @securityService.isAccountOwner(authentication, #id)")
    public ResponseEntity<AccountResponseDTO> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','MANAGER')")
    public ResponseEntity<AccountResponseDTO> updateAccount(@PathVariable UUID id, @RequestBody AccountRequestDTO request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','MANAGER')")
    public ResponseEntity<?> closeAccount(@PathVariable UUID id) {
        accountService.closeAccount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<AccountResponseDTO>> getMyAccounts() {
        return ResponseEntity.ok(accountService.getAccountsForCurrentUser());
    }
}
