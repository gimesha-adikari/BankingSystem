package com.bankingsystem.mobile.features.accounts.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.core.modules.common.storage.DefaultAccountStore
import com.bankingsystem.mobile.features.accounts.domain.errors.CustomerMissingException
import com.bankingsystem.mobile.features.accounts.domain.repository.AccountRepository
import com.bankingsystem.mobile.features.accounts.interfaces.ui.models.AccountUi
import com.bankingsystem.mobile.features.kyc.domain.repository.KycRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyAccountsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val accounts: List<AccountUi> = emptyList(),
    val defaultAccountId: String? = null,
    val customerMissing: Boolean = false,
    val customerMessage: String? = null,
    /** null = unknown/not fetched; true = APPROVED; false = not approved */
    val kycApproved: Boolean? = null
)

@HiltViewModel
class MyAccountsViewModel @Inject constructor(
    private val repo: AccountRepository,
    private val defaults: DefaultAccountStore,
    private val kyc: KycRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(MyAccountsUiState())
    val ui: StateFlow<MyAccountsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            defaults.defaultAccountId.collect { id ->
                _ui.update { it.copy(defaultAccountId = id) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    loading = true,
                    error = null,
                    customerMissing = false,
                    customerMessage = null,
                    // keep prior value until we know otherwise
                    kycApproved = it.kycApproved
                )
            }
            try {
                val list = repo.getMyAccounts().map { acc -> acc.toUi() }
                _ui.update { it.copy(loading = false, accounts = list) }
            } catch (e: CustomerMissingException) {
                // Customer profile not created yet — look up KYC status
                val approved: Boolean? = try {
                    val status = kyc.myCase().status
                    status.equals("APPROVED", ignoreCase = true)
                } catch (_: Exception) {
                    null // unknown
                }
                _ui.update {
                    it.copy(
                        loading = false,
                        accounts = emptyList(),
                        customerMissing = true,
                        customerMessage = e.message,
                        kycApproved = approved
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Failed to load accounts"
                    )
                }
            }
        }
    }

    fun setDefault(id: String) {
        viewModelScope.launch { defaults.setDefaultAccountId(id) }
    }
}
