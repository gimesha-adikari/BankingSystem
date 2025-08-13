package com.bankingsystem.mobile.data.repository

// Import statement for TokenManager, likely a class for managing authentication tokens.
import com.bankingsystem.mobile.data.storage.TokenManager
import com.bankingsystem.mobile.data.config.RetrofitClient
import com.bankingsystem.mobile.data.model.LoginRequest
import com.bankingsystem.mobile.data.model.LoginResponse
import com.bankingsystem.mobile.data.model.RegisterRequest
import com.bankingsystem.mobile.data.model.ValidateTokenResponse
import com.bankingsystem.mobile.data.service.ApiService
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository class for handling user-related data operations.
 *
 * This class is responsible for interacting with the backend API for user authentication,
 * registration, and other user-specific actions. It uses [ApiService] for network requests
 * and [TokenManager] for managing user authentication tokens.
 *
 * @property apiService An instance of [ApiService] used to make network calls to the backend.
 *                      Defaults to the singleton instance provided by [RetrofitClient.apiService].
 * @property tokenManager An instance of [TokenManager] used to save, retrieve, and clear
 *                        authentication tokens.
 */
class UserRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val tokenManager: TokenManager
) {

    /**
     * Attempts to log in a user with the given username and password.
     */
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.loginUser(LoginRequest(username, password))
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    tokenManager.saveToken(loginResponse.token)
                    Result.success(loginResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Login failed: $errorMessage"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }

    /**
     * Logs out the current user by clearing their authentication token.
     */
    suspend fun logout() {
        tokenManager.clearToken()
    }

    /**
     * Checks if a given username is available for registration.
     */
    suspend fun checkUsernameAvailability(username: String): Boolean {
        if (username.length < 3) return false
        return try {
            val response = apiService.checkUsernameAvailability(username)
            when {
                response.isSuccessful -> true
                response.code() == 409 -> false
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Registers a new user with the provided username, email, and password.
     */
    suspend fun registerUser(username: String, email: String, password: String): Boolean {
        return try {
            val request = RegisterRequest(username, email, password)
            val response = apiService.registerUser(request)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Initiates the password recovery process for the given email address.
     */
    suspend fun forgotPassword(email: String): Boolean {
        return try {
            val response = apiService.forgotPassword(email)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun validateToken(token: String): Result<ValidateTokenResponse> {
        return try {
            val response = apiService.validateToken("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Invalid or expired token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * A Flow that emits the current authentication token, allowing observers to react to token changes.
     */
    val tokenFlow = tokenManager.tokenFlow
}
