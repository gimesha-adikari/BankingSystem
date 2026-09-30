package com.bankingsystem.mobile.features.profile.integration.remote.api

import com.bankingsystem.mobile.features.profile.domain.model.UserProfile
import com.bankingsystem.mobile.features.profile.integration.remote.dto.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfileApi {
    @GET("api/v1/users/me")
    suspend fun getMe(): UserProfileDto

    @PUT("api/v1/users/me")
    suspend fun updateMe(
        @Body payload: Map<String, String>
    ): Response<Unit>

    @PUT("/api/v1/auth/change-password")
    suspend fun changePassword(
        @Body body: Map<String, String>
    ): Response<Unit>
}
