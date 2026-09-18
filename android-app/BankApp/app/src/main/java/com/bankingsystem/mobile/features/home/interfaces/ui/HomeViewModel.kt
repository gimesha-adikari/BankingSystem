package com.bankingsystem.mobile.features.home.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.core.modules.common.storage.DefaultAccountStore
import com.bankingsystem.mobile.features.accounts.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val balance: Double = 0.0,
    val defaultAccountId: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: AccountRepository,
    private val defaults: DefaultAccountStore
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            defaults.defaultAccountId.collect { id ->
                _ui.update { it.copy(defaultAccountId = id) }
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val accounts = repo.getMyAccounts()
                val pick = accounts.firstOrNull { it.accountId == _ui.value.defaultAccountId }
                    ?: accounts.firstOrNull()
                _ui.update { it.copy(loading = false, balance = pick?.balance ?: 0.0) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Failed to load") }
            }
        }
    }
}
