package com.bankingsystem.mobile.data.service

import com.bankingsystem.mobile.data.model.RegisterRequest
import com.bankingsystem.mobile.data.model.UserProfile
import com.bankingsystem.mobile.data.model.ValidateTokenResponse
import com.bankingsystem.mobile.data.remote.dto.LoginRequest
import com.bankingsystem.mobile.data.remote.dto.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {

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
        @Query("email") email: String
    ): Response<Unit>

    @GET("api/v1/auth/validate-token")
    suspend fun validateToken(): Response<ValidateTokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/v1/users/me")
    suspend fun getMe(): Response<UserProfile>

    @PUT("api/v1/users/me")
    suspend fun updateMe(
        @Body payload: Map<String, String>
    ): Response<Unit>

    @PUT("/api/v1/auth/change-password")
    suspend fun changePassword(
        @Body body: Map<String, String>
    ): retrofit2.Response<Unit>
}
