package com.bankingsystem.mobile.features.auth.integration.remote.api

import com.bankingsystem.mobile.features.auth.domain.model.ValidateTokenResponse
import com.bankingsystem.mobile.features.auth.integration.remote.dto.LoginRequest
import com.bankingsystem.mobile.features.auth.integration.remote.dto.LoginResponse
import com.bankingsystem.mobile.features.auth.integration.remote.dto.ForgotPasswordRequest
import com.bankingsystem.mobile.features.auth.integration.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {

    @GET("api/v1/auth/available")
    suspend fun checkUsernameAvailability(
        @Query("username") username: String
    ): Response<Unit>

    @POST("api/v1/auth/register")
    suspend fun registerUser(
        @Body registerRequest: RegisterRequest
    ): Response<Unit>

    @POST("api/v1/auth/login")
    suspend fun loginUser(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<Unit>

    @GET("api/v1/auth/validate-token")
    suspend fun validateToken(): Response<ValidateTokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>
}
