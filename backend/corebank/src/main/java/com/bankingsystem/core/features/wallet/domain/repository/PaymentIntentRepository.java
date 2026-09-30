package com.bankingsystem.core.features.wallet.domain.repository;

import com.bankingsystem.core.features.wallet.domain.entity.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, String> {
    Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentIntent> findByIdAndUserId(String id, UUID userId);
}

