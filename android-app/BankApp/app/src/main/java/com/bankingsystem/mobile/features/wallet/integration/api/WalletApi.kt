package com.bankingsystem.mobile.features.wallet.integration.api

import com.bankingsystem.mobile.features.wallet.integration.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface WalletApi {

    @GET("api/v1/wallet/cards")
    suspend fun getCards(): CardListResponseDto

    @POST("api/v1/wallet/cards/session")
    suspend fun createAddCardSession(
        @Query("returnUrl") returnUrl: String
    ): CreateCardSessionResponseDto

    @PATCH("api/v1/wallet/cards/{id}:default")
    suspend fun makeDefault(@Path("id") id: String): Response<Unit>

    @DELETE("api/v1/wallet/cards/{id}")
    suspend fun deleteCard(@Path("id") id: String): Response<Unit>

    @POST("api/v1/wallet/payments/qr")
    suspend fun createQrPayment(
        @Body body: QrPaymentRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String
    ): PaymentIntentDto

    @POST("api/v1/wallet/payments/reload")
    suspend fun createReloadPayment(
        @Body body: ReloadRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String
    ): PaymentIntentDto

    @POST("api/v1/wallet/payments/bill")
    suspend fun createBillPayment(
        @Body body: BillPayRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String
    ): PaymentIntentDto

    @GET("api/v1/wallet/payments/{intentId}")
    suspend fun getPaymentIntent(@Path("intentId") intentId: String): PaymentIntentDto
}
