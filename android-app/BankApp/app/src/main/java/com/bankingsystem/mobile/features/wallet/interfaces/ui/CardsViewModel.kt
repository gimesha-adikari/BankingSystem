package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.features.wallet.domain.model.Card
import com.bankingsystem.mobile.features.wallet.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardsState(
    val loading: Boolean = false,
    val error: String? = null,
    val cards: List<Card> = emptyList()
)

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val repo: WalletRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CardsState())
    val state: StateFlow<CardsState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.getCards()
                .onSuccess { list -> _state.update { it.copy(loading = false, cards = list, error = null) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun makeDefault(id: String) {
        viewModelScope.launch {
            repo.makeDefault(id)
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            refresh()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.deleteCard(id)
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            refresh()
        }
    }

    fun launchAddCardSession(
        onUrl: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repo.startAddCardSession()
                .onSuccess { res -> onUrl(res.url) }
                .onFailure { e -> onError(e.message ?: "error") }
        }
    }
}
