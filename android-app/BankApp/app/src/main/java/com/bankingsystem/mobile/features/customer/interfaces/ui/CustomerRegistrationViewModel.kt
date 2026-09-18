package com.bankingsystem.mobile.features.customer.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.features.customer.domain.model.CustomerRequest
import com.bankingsystem.mobile.features.customer.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerForm(
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val status: String = "ACTIVE"
)

data class CustomerUiState(
    val form: CustomerForm = CustomerForm(),
    val submitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
) {
    val canSubmit: Boolean
        get() = form.firstName.isNotBlank() &&
                form.lastName.isNotBlank()  &&
                form.gender.isNotBlank()    &&
                form.email.isNotBlank()     &&
                form.phone.isNotBlank()     &&
                form.dateOfBirth.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
}

@HiltViewModel
class CustomerRegistrationViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(CustomerUiState())
    val ui: StateFlow<CustomerUiState> = _ui.asStateFlow()

    fun update(block: (CustomerForm) -> CustomerForm) = _ui.update {
        it.copy(form = block(it.form), error = null)
    }

    fun submit() {
        val f = _ui.value.form
        if (!_ui.value.canSubmit || _ui.value.submitting) return

        _ui.update { it.copy(submitting = true, error = null) }

        viewModelScope.launch {
            val req = CustomerRequest(
                firstName = f.firstName.trim(),
                lastName  = f.lastName.trim(),
                gender    = f.gender.trim().uppercase(),
                email     = f.email.trim(),
                phone     = f.phone.trim(),
                address   = f.address.trim().ifBlank { null },
                dateOfBirth = f.dateOfBirth.trim(),
                status    = f.status
            )

            val result = repo.upsertSelf(req)

            _ui.update { state ->
                result.fold(
                    onSuccess = { _ -> state.copy(submitting = false, success = true) },
                    onFailure = { err -> state.copy(submitting = false, error = err.message ?: "Failed to submit") }
                )
            }
        }
    }


    fun clearError() = _ui.update { it.copy(error = null) }
}
