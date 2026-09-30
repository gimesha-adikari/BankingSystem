package com.bankingsystem.core.features.wallet.domain.entity;

import com.bankingsystem.core.features.wallet.domain.PaymentStatus;
import com.bankingsystem.core.features.wallet.domain.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_payment_intents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntent {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "amount_value", nullable = false)
    private double amountValue;

    @Column(name = "amount_currency", nullable = false, length = 8)
    private String amountCurrency;

    @Column(name = "description")
    private String description;

    @Column(name = "merchant_ref")
    private String merchantRef;

    @Column(name = "msisdn")
    private String msisdn;

    @Column(name = "qr_data")
    private String qrData;

    @Column(name = "biller_id")
    private String billerId;

    @Column(name = "reference")
    private String reference;

    @Column(name = "return_url")
    private String returnUrl;

    @Column(name = "provider_client_secret")
    private String providerClientSecret;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
