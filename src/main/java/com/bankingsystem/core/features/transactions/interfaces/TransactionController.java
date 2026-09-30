package com.bankingsystem.core.features.transactions.interfaces;

import com.bankingsystem.core.features.transactions.application.*; import com.bankingsystem.core.modules.common.security.AuthenticatedUserIdProvider; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bankingsystem.core.features.transactions.interfaces.dto.*;
import com.bankingsystem.core.features.transactions.idempotency.domain.IdempotencyKey;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final DepositWorkflow deposits; private final WithdrawalWorkflow withdrawals; private final TransferWorkflow transfers; private final ReversalWorkflow reversals; private final AuthenticatedUserIdProvider principal;

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deposit(@Valid @RequestBody DepositRequest request, @RequestHeader(value="Idempotency-Key", required=false) String key, org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(deposits.deposit(principal.userId(auth), request.accountId(), request.amount(), requireKey(key)));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> withdraw(@Valid @RequestBody WithdrawalRequest request, @RequestHeader(value="Idempotency-Key", required=false) String key, org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(withdrawals.withdraw(principal.userId(auth), request.accountId(), request.amount(), requireKey(key)));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequest request, @RequestHeader(value="Idempotency-Key", required=false) String key, org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(transfers.transfer(principal.userId(auth), request.sourceAccountId(), request.destinationAccountId(), request.amount(), requireKey(key)));
    }

    @PostMapping("/{journalEntryId}/reversal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReversalResponse> reverse(
            @PathVariable java.util.UUID journalEntryId,
            @Valid @RequestBody ReversalRequest request,
            org.springframework.security.core.Authentication auth) {
        ReversalReceipt receipt = reversals.reverse(principal.userId(auth), journalEntryId, request.reason());
        return ResponseEntity.ok(ReversalResponse.from(receipt));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public ResponseEntity<?> listTransactions() {
        return ResponseEntity.ok("All transactions");
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyTransactions() {
        return ResponseEntity.ok("User's transactions");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER') or @securityService.isTransactionOwner(authentication, #id)")
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok("Transaction details");
    }
    private String requireKey(String key) { IdempotencyKey.of(key); return key; }
}
