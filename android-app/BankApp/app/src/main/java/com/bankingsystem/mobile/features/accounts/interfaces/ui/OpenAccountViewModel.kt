package com.bankingsystem.mobile.features.accounts.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.features.accounts.domain.errors.ApiCallException
import com.bankingsystem.mobile.features.accounts.domain.errors.CustomerMissingException
import com.bankingsystem.mobile.features.accounts.domain.repository.AccountRepository
import com.bankingsystem.mobile.features.accounts.interfaces.ui.models.AccountType
import com.bankingsystem.mobile.features.accounts.interfaces.ui.models.BranchOption
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OpenAccountUiState (
    val accountType: AccountType = AccountType.SAVINGS,
    val initialDeposit: String = "",
    val selectedBranchId: String? = null,
    val branches: List<BranchOption> = emptyList(),
    val loadingBranches: Boolean = false,
    val submitting: Boolean = false,
    val depositError: String? = null,
    val branchError: String? = null
)

sealed interface OpenAccountEvent {
    data class Created(val accountId: String, val accountNumber: String) : OpenAccountEvent
    data class Error(val message: String) : OpenAccountEvent
    data object NeedsCustomerProfile : OpenAccountEvent
}

@HiltViewModel
class OpenAccountViewModel @Inject constructor(
    private val repo: AccountRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(OpenAccountUiState())
    val ui: StateFlow<OpenAccountUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<OpenAccountEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OpenAccountEvent> = _events.asSharedFlow()

    private val minDeposit = mapOf(
        AccountType.SAVINGS to 1000.0,
        AccountType.CHECKING to 0.0,
        AccountType.FIXED_DEPOSIT to 5000.0
    )

    init { loadBranches() }

    fun onAccountTypeChange(type: AccountType) =
        _ui.update { it.copy(accountType = type, depositError = null) }

    fun onInitialDepositChange(value: String) =
        _ui.update { it.copy(initialDeposit = value, depositError = null) }

    fun onBranchChange(id: String?) =
        _ui.update { it.copy(selectedBranchId = id, branchError = null) }

    fun loadBranches() = viewModelScope.launch {
        _ui.update { it.copy(loadingBranches = true) }
        try {
            val branches = repo.getBranches()
            val options = branches.map { BranchOption(id = it.branchId, name = it.branchName) }
            _ui.update { st ->
                st.copy(
                    branches = options,
                    loadingBranches = false,
                    selectedBranchId = st.selectedBranchId?.takeIf { id -> options.any { it.id == id } }
                )
            }
        } catch (_: Exception) {
            _ui.update { it.copy(loadingBranches = false) }
            _events.tryEmit(OpenAccountEvent.Error("Failed to load branches"))
        }
    }

    fun submit() = viewModelScope.launch {
        val st = _ui.value
        var depositErr: String? = null
        var branchErr: String? = null

        val amount = st.initialDeposit.toDoubleOrNull()
        if (amount == null || amount < 0.0) {
            depositErr = "Enter a valid non-negative amount"
        } else {
            val min = minDeposit[st.accountType]!!
            if (amount < min) depositErr = "Minimum for ${st.accountType.pretty()} is ${"%,.2f".format(min)}"
        }
        if (st.selectedBranchId.isNullOrBlank()) branchErr = "Select a branch"

        if (depositErr != null || branchErr != null) {
            _ui.update { it.copy(depositError = depositErr, branchError = branchErr) }
            return@launch
        }

        _ui.update { it.copy(submitting = true, depositError = null, branchError = null) }
        try {
            val res = repo.openAccount(
                accountType = st.accountType.name,
                initialDeposit = st.initialDeposit.toDouble(),
                branchId = st.selectedBranchId!!
            )
            _events.tryEmit(OpenAccountEvent.Created(res.accountId, res.accountNumber))
            _ui.update { it.copy(accountType = AccountType.SAVINGS, initialDeposit = "", selectedBranchId = null, submitting = false) }
        } catch (e: Exception) {
            _ui.update { it.copy(submitting = false) }
            when (e) {
                is CustomerMissingException -> _events.tryEmit(OpenAccountEvent.NeedsCustomerProfile)
                is ApiCallException -> _events.tryEmit(OpenAccountEvent.Error(e.message ?: "Failed to open account"))
                else -> _events.tryEmit(OpenAccountEvent.Error("Failed to open account"))
            }
        }
    }
}

private fun AccountType.pretty() =
    name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
