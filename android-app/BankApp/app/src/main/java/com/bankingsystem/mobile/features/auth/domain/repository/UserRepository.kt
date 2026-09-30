package com.bankingsystem.mobile.features.auth.domain.repository

import com.bankingsystem.mobile.features.auth.domain.model.ValidateTokenResponse
import com.bankingsystem.mobile.features.auth.integration.remote.dto.LoginResponse
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(username: String, password: String): Result<LoginResponse>
    suspend fun logout()
    suspend fun checkUsernameAvailability(username: String): Boolean
    suspend fun registerUser(username: String, email: String, password: String): Boolean
    suspend fun forgotPassword(email: String): Boolean
    suspend fun validateToken(): Result<ValidateTokenResponse>
    val tokenFlow: Flow<String?>
}
