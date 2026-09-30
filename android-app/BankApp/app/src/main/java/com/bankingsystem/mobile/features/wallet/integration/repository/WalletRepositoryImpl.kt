package com.bankingsystem.mobile.features.wallet.integration.repository

import com.bankingsystem.mobile.features.wallet.domain.model.*
import com.bankingsystem.mobile.features.wallet.domain.repository.WalletRepository
import com.bankingsystem.mobile.features.wallet.integration.api.WalletApi
import com.bankingsystem.mobile.features.wallet.integration.remote.dto.*
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val api: WalletApi
) : WalletRepository {

    override suspend fun getCards(): Result<List<Card>> = runCatching {
        api.getCards().cards.map { it.toDomain() }
    }

    override suspend fun startAddCardSession(): Result<CreateCardSessionResult> = runCatching {
        val deepLink = "bankapp://wallet/card/return"
        val dto = api.createAddCardSession(deepLink)
        CreateCardSessionResult(dto.sessionId, dto.url)
    }

    override suspend fun makeDefault(cardId: String): Result<Unit> = runCatching {
        api.makeDefault(cardId)
        Unit
    }

    override suspend fun deleteCard(cardId: String): Result<Unit> = runCatching {
        api.deleteCard(cardId)
        Unit
    }

    override suspend fun createQrPayment(
        amount: Amount,
        qrData: String?,
        merchantRef: String?,
        idemKey: String?
    ): Result<PaymentIntent> = runCatching {
        val payloadQr = qrData ?: buildQrData(
            merchant = merchantRef.orEmpty(),
            memo = "",
            amount = amount.value
        )
        val key = idemKey ?: UUID.randomUUID().toString()
        api.createQrPayment(
            QrPaymentRequestDto(
                amount = amount.toDto(),
                qrData = payloadQr,
                merchantRef = merchantRef
            ),
            key
        ).toDomain()
    }

    override suspend fun createReloadPayment(
        msisdn: String,
        amount: Amount,
        idemKey: String?
    ): Result<PaymentIntent> = runCatching {
        val key = idemKey ?: UUID.randomUUID().toString()
        api.createReloadPayment(
            ReloadRequestDto(msisdn = msisdn, amount = amount.toDto()),
            key
        ).toDomain()
    }

    override suspend fun createBillPayment(
        billerId: String,
        reference: String,
        amount: Amount,
        idemKey: String?
    ): Result<PaymentIntent> = runCatching {
        val key = idemKey ?: UUID.randomUUID().toString()
        api.createBillPayment(
            BillPayRequestDto(billerId = billerId, reference = reference, amount = amount.toDto()),
            key
        ).toDomain()
    }

    override suspend fun getPaymentIntent(intentId: String): Result<PaymentIntent> = runCatching {
        api.getPaymentIntent(intentId).toDomain()
    }

    private fun buildQrData(merchant: String, memo: String, amount: Double): String {
        val enc = { s: String -> URLEncoder.encode(s, "UTF-8") }
        return "merchantName=${enc(merchant)}&memo=${enc(memo)}&amount=${enc(amount.toString())}"
    }
}
