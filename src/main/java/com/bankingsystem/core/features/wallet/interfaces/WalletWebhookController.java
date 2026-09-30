package com.bankingsystem.core.features.wallet.interfaces;

import com.bankingsystem.core.features.wallet.domain.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Generic status-mutation webhook — DISABLED (Phase 4A P0-2).
 *
 * This endpoint previously accepted unsigned status mutations from any
 * authenticated user, allowing any user to set any payment intent to SUCCESS.
 * No legitimate runtime consumer exists (confirmed by full-project search).
 *
 * Authoritative payment status comes from PayHere's signed callback at
 * POST /api/v1/wallet/payhere/notify (MD5 verified).
 *
 * Returns HTTP 410 Gone to all callers.
 */
@RestController
@RequestMapping("/api/v1/wallet/webhook")
@RequiredArgsConstructor
public class WalletWebhookController {

    @SuppressWarnings("unused")
    private final PaymentIntentRepository intents;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestParam String intentId, @RequestParam String status) {
        return ResponseEntity.status(410).build();
    }
}
