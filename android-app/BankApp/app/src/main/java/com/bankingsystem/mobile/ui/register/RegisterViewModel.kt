package com.bankingsystem.mobile.ui.register

import com.bankingsystem.mobile.data.storage.TokenManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.data.config.RetrofitClient
import com.bankingsystem.mobile.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Represents the different states of the registration process.
 */
sealed class RegisterState {
    /** Indicates that the registration process has not started yet. */
    object Idle : RegisterState()
    /** Indicates that the registration process is ongoing. */
    object Loading : RegisterState()
    /** Indicates that the registration was successful.
     * @param message A message confirming the success. */
    data class Success(val message: String) : RegisterState()
    /** Indicates that an error occurred during registration.
     * @param error A message describing the error. */
    data class Error(val error: String) : RegisterState()
}

/** ViewModel for the registration screen. Handles user registration and username availability checks. */
class RegisterViewModel(
    context: Context,
    private val repository: UserRepository = UserRepository(
        apiService = RetrofitClient.apiService,
        tokenManager = TokenManager(context)
    )
) : ViewModel() {

    // StateFlow to hold the availability status of a username.
    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    /** Observable StateFlow that emits the availability status of the username. */
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable

    // StateFlow to hold the current state of the registration process.
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    /** Observable StateFlow that emits the current state of the registration process. */
    val registerState: StateFlow<RegisterState> = _registerState

    private var checkUsernameJob: Job? = null

    /** Checks if a given username is available. It includes a debounce mechanism. */
    fun checkUsernameAvailability(username: String) {
        checkUsernameJob?.cancel()
        _usernameAvailable.value = null
        if (username.length < 3) {
            _usernameAvailable.value = false
            return
        }
        checkUsernameJob = viewModelScope.launch {
            delay(500) // debounce
            val available = repository.checkUsernameAvailability(username)
            _usernameAvailable.value = available
        }
    }

    /** Attempts to register a new user with the provided credentials. */
    fun registerUser(username: String, email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            val success = repository.registerUser(username, email, password)
            if (success) {
                _registerState.value = RegisterState.Success("Registration successful")
            } else {
                _registerState.value = RegisterState.Error("Registration failed, please try again")
            }
        }
    }

    /** Resets the registration state back to Idle. */
    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }

}
