package com.bankingsystem.mobile.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.data.config.RetrofitClient
import com.bankingsystem.mobile.data.repository.UserRepository
import com.bankingsystem.mobile.data.storage.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Represents the different states of the login process.
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String, val username: String, val role: String) : LoginState()
    data class Error(val error: String) : LoginState()
}

/**
 * Represents the different states of the forgot password process.
 */
sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
    data class Error(val error: String) : ForgotPasswordState()
}

/**
 * ViewModel for the Login screen.
 *
 * Handles login / auto-login / forgot password / logout.
 * NOTE: Authorization header is injected globally by AuthInterceptor (via RetrofitClient.init()).
 */
class LoginViewModel(
    context: Context,
    private val userRepository: UserRepository = UserRepository(
        apiService = RetrofitClient.apiService,
        tokenManager = TokenManager(context)
    )
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState

    /**
     * Manual login with username/password.
     * Repository saves the token internally; no need to save it again here.
     */
    fun loginUser(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Username or password cannot be blank")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = userRepository.login(username.trim(), password)
            result.fold(
                onSuccess = { loginResponse ->
                    _loginState.value = LoginState.Success(
                        token = loginResponse.token,
                        username = loginResponse.username,
                        role = loginResponse.role
                    )
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    /**
     * Auto-login by observing the token flow from the repository.
     * If a token exists, validate it (no-arg; header via interceptor).
     */
    fun autoLogin() {
        viewModelScope.launch {
            userRepository.tokenFlow.collect { token ->
                if (!token.isNullOrBlank()) {
                    _loginState.value = LoginState.Loading
                    val result = userRepository.validateToken()
                    result.fold(
                        onSuccess = { validated ->
                            _loginState.value = LoginState.Success(
                                token = token,
                                username = validated.username,
                                role = validated.role
                            )
                        },
                        onFailure = {
                            // If token invalid, repository will not clear it automatically here.
                            // We can clear it by calling logout() or expose a clear method on repo if needed.
                            userRepository.logout()
                            _loginState.value = LoginState.Idle
                        }
                    )
                } else {
                    _loginState.value = LoginState.Idle
                }
            }
        }
    }

    /**
     * Forgot password (backend sends email).
     */
    fun forgotPassword(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Invalid email address")
            return
        }

        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordState.Loading
            try {
                val ok = userRepository.forgotPassword(email.trim())
                if (ok) {
                    _forgotPasswordState.value = ForgotPasswordState.Success("Password reset email sent successfully")
                } else {
                    _forgotPasswordState.value = ForgotPasswordState.Error("Failed to send reset email")
                }
            } catch (e: Exception) {
                _forgotPasswordState.value = ForgotPasswordState.Error(e.message ?: "Network error occurred")
            }
        }
    }

    /**
     * Logout: call repo.logout() (server logout if available, then clear token).
     * Sets UI state to Idle so UI can navigate to Login screen.
     */
    fun logoutUser() {
        viewModelScope.launch {
            try { userRepository.logout() } catch (_: Exception) {}
            _loginState.value = LoginState.Idle
        }
    }

    fun logout() = logoutUser()

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }
}
