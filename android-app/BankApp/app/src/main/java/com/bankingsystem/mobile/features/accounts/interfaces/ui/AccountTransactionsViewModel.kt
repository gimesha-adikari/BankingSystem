package com.bankingsystem.mobile.features.accounts.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.features.accounts.domain.model.Transaction
import com.bankingsystem.mobile.features.accounts.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountTxUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<Transaction> = emptyList()
)

@HiltViewModel
class AccountTransactionsViewModel @Inject constructor(
    private val repo: AccountRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AccountTxUiState(loading = true))
    val ui: StateFlow<AccountTxUiState> = _ui.asStateFlow()

    private var currentAccountId: String? = null

    fun load(accountId: String) {
        if (currentAccountId == accountId && _ui.value.items.isNotEmpty()) return
        currentAccountId = accountId
        refresh()
    }

    fun refresh() {
        val id = currentAccountId ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            runCatching { repo.getAccountTransactions(id) }
                .onSuccess { list -> _ui.value = AccountTxUiState(loading = false, items = list) }
                .onFailure { e -> _ui.value = AccountTxUiState(loading = false, error = e.message ?: "Failed to load transactions") }
        }
    }
}
