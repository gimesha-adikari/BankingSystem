package com.bankingsystem.mobile.features.auth.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.features.auth.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String, val username: String, val role: String) : LoginState()
    data class Error(val error: String) : LoginState()
}

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
    data class Error(val error: String) : ForgotPasswordState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState

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

    fun logoutUser() {
        viewModelScope.launch {
            try { userRepository.logout() } catch (_: Exception) {}
            _loginState.value = LoginState.Idle
        }
    }

    fun logout() = logoutUser()
    fun resetLoginState() { _loginState.value = LoginState.Idle }
    fun resetForgotPasswordState() { _forgotPasswordState.value = ForgotPasswordState.Idle }
}
