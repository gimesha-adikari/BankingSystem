package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillerUi(val id: String, val name: String)

data class BillersState(
    val query: String = "",
    val loading: Boolean = false,
    val items: List<BillerUi> = emptyList()
)

class BillersViewModel : ViewModel() {
    private val _state = MutableStateFlow(BillersState())
    val state: StateFlow<BillersState> = _state

    private val seed = listOf(
        BillerUi("elc_001", "Electricity Board"),
        BillerUi("wtr_002", "Water Supply"),
        BillerUi("mob_003", "Mobitel Postpaid"),
        BillerUi("dsl_004", "Fiber Internet"),
        BillerUi("ins_005", "Life Insurance")
    )

    init {
        viewModelScope.launch {
            _state.update { it.copy(items = seed) }
        }
    }

    fun setQuery(q: String) {
        val f = if (q.isBlank()) seed else seed.filter { it.name.contains(q, ignoreCase = true) || it.id.contains(q, ignoreCase = true) }
        _state.update { it.copy(query = q, items = f) }
    }
}
