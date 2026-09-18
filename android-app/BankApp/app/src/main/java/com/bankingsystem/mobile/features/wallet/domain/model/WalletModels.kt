package com.bankingsystem.mobile.features.wallet.domain.model

data class Card(
    val id: String,
    val brand: String,
    val last4: String,
    val isDefault: Boolean,
    val createdAt: String
)

data class Amount(
    val value: Double,
    val currency: String
)

enum class PaymentStatus { PENDING, PROCESSING, SUCCESS, FAILED, CANCELED }

data class PaymentIntent(
    val intentId: String,
    val status: PaymentStatus,
    val amount: Amount,
    val description: String? = null,
    val returnUrl: String? = null,
    val providerClientSecret: String? = null
)

data class CreateCardSessionResult(
    val sessionId: String,
    val url: String
)
