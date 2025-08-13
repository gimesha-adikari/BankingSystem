package com.bankingsystem.mobile.ui.login

import com.bankingsystem.mobile.data.storage.TokenManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.data.config.RetrofitClient
import com.bankingsystem.mobile.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Represents the different states of the login process.
 */
sealed class LoginState {
    /** Represents the initial or idle state. */
    object Idle : LoginState()
    /** Represents the state when login is in progress. */
    object Loading : LoginState()
    /**
     * Represents a successful login.
     * @property token The authentication token.
     * @property username The username of the logged-in user.
     * @property role The role of the logged-in user.
     */
    data class Success(val token: String, val username: String, val role: String) : LoginState()
    /**
     * Represents an error during login.
     * @property error The error message.
     */
    data class Error(val error: String) : LoginState()
}

/**
 * Represents the different states of the forgot password process.
 */
sealed class ForgotPasswordState {
    /** Represents the initial or idle state. */
    object Idle : ForgotPasswordState()
    /** Represents the state when the forgot password request is in progress. */
    object Loading : ForgotPasswordState()
    /**
     * Represents a successful forgot password request.
     * @property message The success message.
     */
    data class Success(val message: String) : ForgotPasswordState()
    /**
     * Represents an error during the forgot password request.
     * @property error The error message.
     */
    data class Error(val error: String) : ForgotPasswordState()
}

/**
 * ViewModel for the Login screen.
 *
 * This ViewModel handles the logic for user login, password recovery, and logout.
 * It interacts with the [UserRepository] to perform these operations and updates
 * the UI state through [LoginState] and [ForgotPasswordState].
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

    val tokenManager = TokenManager(context)

    fun loginUser(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Username or password cannot be blank")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = userRepository.login(username.trim(), password)
                result.fold(
                    onSuccess = { loginResponse ->
                        viewModelScope.launch {
                            tokenManager.saveToken(loginResponse.token)
                        }
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
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Network error occurred")
            }
        }
    }

    fun autoLogin() {
        viewModelScope.launch {
            tokenManager.tokenFlow.collect { token ->
                if (!token.isNullOrBlank()) {
                    _loginState.value = LoginState.Loading
                    val result = userRepository.validateToken(token)
                    result.fold(
                        onSuccess = { validated ->
                            _loginState.value = LoginState.Success(
                                token = token,
                                username = validated.username,
                                role = validated.role
                            )
                        },
                        onFailure = {
                            tokenManager.clearToken()
                            _loginState.value = LoginState.Idle
                        }
                    )
                } else {
                    _loginState.value = LoginState.Idle
                }
            }
        }
    }


    fun forgotPassword(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            _forgotPasswordState.value = ForgotPasswordState.Error("Invalid email address")
            return
        }

        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordState.Loading
            try {
                val response = userRepository.forgotPassword(email.trim())
                if (response) {
                    _forgotPasswordState.value = ForgotPasswordState.Success("Password reset email sent successfully")
                } else {
                    _forgotPasswordState.value = ForgotPasswordState.Error("Failed to send reset email")
                }
            } catch (e: Exception) {
                _forgotPasswordState.value = ForgotPasswordState.Error(e.message ?: "Network error occurred")
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            userRepository.logout()
            _loginState.value = LoginState.Idle
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }
}
