package com.bankingsystem.core.features.wallet.interfaces;

import com.bankingsystem.core.features.wallet.domain.PaymentStatus;
import com.bankingsystem.core.features.wallet.domain.entity.PaymentIntent;
import com.bankingsystem.core.features.wallet.domain.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet/webhook")
@RequiredArgsConstructor
public class WalletWebhookController {

    private final PaymentIntentRepository intents;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestParam String intentId, @RequestParam String status) {
        PaymentIntent p = intents.findById(intentId).orElse(null);
        if (p == null) return ResponseEntity.ok().build();
        p.setStatus(PaymentStatus.valueOf(status.toUpperCase()));
        intents.save(p);
        return ResponseEntity.ok().build();
    }
}
