package com.bankingsystem.mobile.features.wallet.domain.repository

import com.bankingsystem.mobile.features.wallet.domain.model.*

interface WalletRepository {
    // Cards
    suspend fun getCards(): Result<List<Card>>
    suspend fun startAddCardSession(): Result<CreateCardSessionResult>
    suspend fun makeDefault(cardId: String): Result<Unit>
    suspend fun deleteCard(cardId: String): Result<Unit>

    // Payments
    suspend fun createQrPayment(amount: Amount, qrData: String?, merchantRef: String?, idemKey: String? = null): Result<PaymentIntent>
    suspend fun createReloadPayment(msisdn: String, amount: Amount, idemKey: String? = null): Result<PaymentIntent>
    suspend fun createBillPayment(billerId: String, reference: String, amount: Amount, idemKey: String? = null): Result<PaymentIntent>
    suspend fun getPaymentIntent(intentId: String): Result<PaymentIntent>
}
