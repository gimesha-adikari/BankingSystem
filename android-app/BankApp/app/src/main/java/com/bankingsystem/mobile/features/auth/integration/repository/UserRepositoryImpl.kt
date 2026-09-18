package com.bankingsystem.mobile.features.auth.integration.repository

import com.bankingsystem.mobile.core.modules.common.storage.TokenManager
import com.bankingsystem.mobile.features.auth.domain.model.ValidateTokenResponse
import com.bankingsystem.mobile.features.auth.domain.repository.UserRepository
import com.bankingsystem.mobile.features.auth.integration.remote.api.AuthApi
import com.bankingsystem.mobile.features.auth.integration.remote.dto.LoginRequest
import com.bankingsystem.mobile.features.auth.integration.remote.dto.LoginResponse
import com.bankingsystem.mobile.features.auth.integration.remote.dto.ForgotPasswordRequest
import com.bankingsystem.mobile.features.auth.integration.remote.dto.RegisterRequest
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenManager: TokenManager
) : UserRepository {

    override suspend fun login(username: String, password: String): Result<LoginResponse> = try {
        val response = api.loginUser(LoginRequest(username, password))
        if (response.isSuccessful) {
            response.body()?.let { body ->
                tokenManager.saveToken(body.token)
                Result.success(body)
            } ?: Result.failure(IllegalStateException("Empty response body"))
        } else {
            val error = response.errorBody()?.string() ?: "Unknown error"
            Result.failure(IllegalStateException("Login failed: $error"))
        }
    } catch (e: IOException) {
        Result.failure(IOException("Network error: ${e.message}"))
    } catch (e: HttpException) {
        Result.failure(IllegalStateException("Server error: ${e.message}"))
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Unexpected error: ${e.message}"))
    }

    override suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        finally { tokenManager.clearToken() }
    }

    override suspend fun checkUsernameAvailability(username: String): Boolean {
        if (username.length < 3) return false
        return try {
            val res = api.checkUsernameAvailability(username)
            when {
                res.isSuccessful -> true
                res.code() == 409 -> false
                else -> false
            }
        } catch (_: Exception) { false }
    }

    override suspend fun registerUser(username: String, email: String, password: String): Boolean = try {
        api.registerUser(RegisterRequest(username, email, password)).isSuccessful
    } catch (_: Exception) { false }

    override suspend fun forgotPassword(email: String): Boolean = try {
        api.forgotPassword(ForgotPasswordRequest(email)).isSuccessful
    } catch (_: Exception) { false }

    override suspend fun validateToken(): Result<ValidateTokenResponse> = try {
        val res = api.validateToken()
        if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
        else Result.failure(IllegalStateException("Invalid or expired token"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override val tokenFlow: Flow<String?> = tokenManager.tokenFlow
}
