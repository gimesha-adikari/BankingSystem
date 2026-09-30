package com.bankingsystem.mobile.features.wallet.integration.remote.dto

import com.bankingsystem.mobile.features.wallet.domain.model.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardDto(
    @Json(name = "id")        val id: String,
    @Json(name = "brand")     val brand: String,
    @Json(name = "last4")     val last4: String,
    @Json(name = "isDefault") val isDefault: Boolean,
    @Json(name = "createdAt") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CardListResponseDto(
    @Json(name = "cards") val cards: List<CardDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateCardSessionResponseDto(
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "url")       val url: String
)

@JsonClass(generateAdapter = true)
data class AmountDto(
    @Json(name = "value")    val value: Double,
    @Json(name = "currency") val currency: String
)

enum class PaymentStatusDto { PENDING, PROCESSING, SUCCESS, FAILED, CANCELED }

@JsonClass(generateAdapter = true)
data class PaymentIntentDto(
    @Json(name = "intentId")             val intentId: String,
    @Json(name = "status")               val status: PaymentStatusDto,
    @Json(name = "amount")               val amount: AmountDto,
    @Json(name = "description")          val description: String? = null,
    @Json(name = "returnUrl")            val returnUrl: String? = null,
    @Json(name = "providerClientSecret") val providerClientSecret: String? = null
)

@JsonClass(generateAdapter = true)
data class QrPaymentRequestDto(
    @Json(name = "amount")      val amount: AmountDto,
    @Json(name = "qrData")      val qrData: String? = null,
    @Json(name = "merchantRef") val merchantRef: String? = null
)

@JsonClass(generateAdapter = true)
data class ReloadRequestDto(
    @Json(name = "msisdn") val msisdn: String,
    @Json(name = "amount") val amount: AmountDto
)

@JsonClass(generateAdapter = true)
data class BillPayRequestDto(
    @Json(name = "billerId")  val billerId: String,
    @Json(name = "reference") val reference: String,
    @Json(name = "amount")    val amount: AmountDto
)

fun CardDto.toDomain() = Card(id, brand, last4, isDefault, createdAt)

fun AmountDto.toDomain() = Amount(value, currency)
fun Amount.toDto() = AmountDto(value, currency)

fun PaymentIntentDto.toDomain() = PaymentIntent(
    intentId = intentId,
    status = when (status) {
        PaymentStatusDto.PENDING -> PaymentStatus.PENDING
        PaymentStatusDto.PROCESSING -> PaymentStatus.PROCESSING
        PaymentStatusDto.SUCCESS -> PaymentStatus.SUCCESS
        PaymentStatusDto.FAILED -> PaymentStatus.FAILED
        PaymentStatusDto.CANCELED -> PaymentStatus.CANCELED
    },
    amount = amount.toDomain(),
    description = description,
    returnUrl = returnUrl,
    providerClientSecret = providerClientSecret
)
