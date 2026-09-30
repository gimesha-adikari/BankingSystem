package com.bankingsystem.mobile.features.wallet.integration.repository

import com.bankingsystem.mobile.features.wallet.domain.model.Amount
import com.bankingsystem.mobile.features.wallet.domain.model.Card
import com.bankingsystem.mobile.features.wallet.domain.model.CreateCardSessionResult
import com.bankingsystem.mobile.features.wallet.domain.model.PaymentIntent
import com.bankingsystem.mobile.features.wallet.domain.model.PaymentStatus
import com.bankingsystem.mobile.features.wallet.domain.repository.WalletRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay

class FakeWalletRepository @Inject constructor() : WalletRepository {

    private val cards = mutableListOf(
        Card(id = "card_1", brand = "VISA", last4 = "4242", isDefault = true, createdAt = "2024-01-01T00:00:00Z"),
        Card(id = "card_2", brand = "MC", last4 = "4444", isDefault = false, createdAt = "2024-01-01T00:00:00Z")
    )

    private val intents = mutableMapOf<String, PaymentIntent>()
    private val intentSteps = mutableMapOf<String, Int>()
    private val intentsByIdem = mutableMapOf<String, String>()

    override suspend fun getCards(): Result<List<Card>> = runCatching {
        delay(300)
        cards.toList()
    }

    override suspend fun startAddCardSession(): Result<CreateCardSessionResult> = runCatching {
        delay(200)
        val id = UUID.randomUUID().toString()
        CreateCardSessionResult(sessionId = id, url = "https://example.com/add-card?session=$id")
    }

    override suspend fun makeDefault(cardId: String): Result<Unit> = runCatching {
        delay(200)
        for (i in cards.indices) {
            val c = cards[i]
            cards[i] = c.copy(isDefault = c.id == cardId)
        }
        Unit
    }

    override suspend fun deleteCard(cardId: String): Result<Unit> = runCatching {
        delay(200)
        cards.removeAll { it.id == cardId }
        if (cards.none { it.isDefault } && cards.isNotEmpty()) {
            cards[0] = cards[0].copy(isDefault = true)
        }
        Unit
    }

    override suspend fun createQrPayment(
        amount: Amount,
        qrData: String?,
        merchantRef: String?,
        idemKey: String?
    ): Result<PaymentIntent> = createPayment(amount, idemKey)

    override suspend fun createReloadPayment(
        msisdn: String,
        amount: Amount,
        idemKey: String?
    ): Result<PaymentIntent> = createPayment(amount, idemKey)

    override suspend fun createBillPayment(
        billerId: String,
        reference: String,
        amount: Amount,
        idemKey: String?
    ): Result<PaymentIntent> = createPayment(amount, idemKey)

    override suspend fun getPaymentIntent(intentId: String): Result<PaymentIntent> = runCatching {
        delay(400)
        val step = (intentSteps[intentId] ?: 0) + 1
        intentSteps[intentId] = step
        val current = intents[intentId] ?: PaymentIntent(
            intentId = intentId,
            status = PaymentStatus.PENDING,
            amount = Amount(0.0, "LKR"),
            description = null,
            returnUrl = null,
            providerClientSecret = null
        )
        val next = when {
            step <= 1 -> current.copy(status = PaymentStatus.PENDING)
            step == 2 -> current.copy(status = PaymentStatus.PROCESSING)
            else -> current.copy(status = PaymentStatus.SUCCESS)
        }
        intents[intentId] = next
        next
    }

    private fun createPayment(amount: Amount, idemKey: String?): Result<PaymentIntent> = runCatching {
        val existingId = idemKey?.let { intentsByIdem[it] }
        if (existingId != null) return@runCatching intents[existingId]!!
        val id = UUID.randomUUID().toString()
        val pi = PaymentIntent(
            intentId = id,
            status = PaymentStatus.PENDING,
            amount = amount,
            description = null,
            returnUrl = null,
            providerClientSecret = null
        )
        intents[id] = pi
        intentSteps[id] = 0
        if (idemKey != null) intentsByIdem[idemKey] = id
        pi
    }
}
