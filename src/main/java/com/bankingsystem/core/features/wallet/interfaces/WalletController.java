package com.bankingsystem.core.features.wallet.interfaces;

import com.bankingsystem.core.features.wallet.application.WalletService;
import com.bankingsystem.core.features.wallet.interfaces.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class WalletController {

    private final WalletService service;

    @GetMapping("/cards")
    @PreAuthorize("isAuthenticated()")
    public CardListResponse cards(Authentication auth) {
        List<CardDto> list = service.listCards(auth);
        return CardListResponse.builder().cards(list).build();
    }

    @PostMapping("/cards/session")
    @PreAuthorize("isAuthenticated()")
    public CreateCardSessionResponse createAddCardSession(@RequestParam(defaultValue = "https://example.com/wallet/card/return") String returnUrl,
                                                          Authentication auth) {
        return service.startAddCardSession(auth, returnUrl);
    }

    @PatchMapping("/cards/{id}:default")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> makeDefault(@PathVariable String id, Authentication auth) {
        service.makeDefault(auth, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cards/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCard(@PathVariable String id, Authentication auth) {
        service.deleteCard(auth, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/payments/qr")
    @PreAuthorize("isAuthenticated()")
    public PaymentIntentDto createQrPayment(@RequestBody QrPaymentRequest req,
                                            @RequestHeader(name = "Idempotency-Key", required = false) String idemKey,
                                            Authentication auth) {
        return service.createQrPayment(auth, req, idemKey);
    }

    @PostMapping("/payments/reload")
    @PreAuthorize("isAuthenticated()")
    public PaymentIntentDto createReloadPayment(@RequestBody ReloadRequest req,
                                                @RequestHeader(name = "Idempotency-Key", required = false) String idemKey,
                                                Authentication auth) {
        return service.createReloadPayment(auth, req, idemKey);
    }

    @PostMapping("/payments/bill")
    @PreAuthorize("isAuthenticated()")
    public PaymentIntentDto createBillPayment(@RequestBody BillPayRequest req,
                                              @RequestHeader(name = "Idempotency-Key", required = false) String idemKey,
                                              Authentication auth) {
        return service.createBillPayment(auth, req, idemKey);
    }

    @GetMapping("/payments/{intentId}")
    @PreAuthorize("isAuthenticated()")
    public PaymentIntentDto getPaymentIntent(@PathVariable String intentId, Authentication auth) {
        return service.getPaymentIntent(auth, intentId);
    }
}
