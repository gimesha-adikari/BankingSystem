package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.features.wallet.domain.model.Amount
import com.bankingsystem.mobile.features.wallet.domain.model.PaymentIntent
import com.bankingsystem.mobile.features.wallet.domain.model.PaymentStatus
import com.bankingsystem.mobile.features.wallet.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PaymentFlowViewModel @Inject constructor(
    private val repo: WalletRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentFlowState())
    val state: StateFlow<PaymentFlowState> = _state

    fun startQr(preview: QrPreview, idemKey: String = UUID.randomUUID().toString()) {
        if (_state.value.stage == PaymentStage.PROCESSING || _state.value.stage == PaymentStage.CREATING) return
        viewModelScope.launch {
            _state.value = PaymentFlowState(stage = PaymentStage.CREATING)
            val amt = Amount(parseAmount(preview.amount), "LKR")
            val res = repo.createQrPayment(amt, preview.merchantName, preview.memo, idemKey)
            res.onFailure { e ->
                _state.value = PaymentFlowState(stage = PaymentStage.FAILED, error = e.message ?: "error")
            }.onSuccess { intent ->
                _state.value = PaymentFlowState(stage = PaymentStage.PROCESSING, intentId = intent.intentId)
                poll(intent.intentId)
            }
        }
    }

    fun startReload(msisdn: String, amountText: String, idemKey: String = UUID.randomUUID().toString()) {
        if (_state.value.stage == PaymentStage.PROCESSING || _state.value.stage == PaymentStage.CREATING) return
        viewModelScope.launch {
            _state.value = PaymentFlowState(stage = PaymentStage.CREATING)
            val amt = Amount(parseAmount(amountText), "LKR")
            val res = repo.createReloadPayment(msisdn, amt, idemKey)
            res.onFailure { e ->
                _state.value = PaymentFlowState(stage = PaymentStage.FAILED, error = e.message ?: "error")
            }.onSuccess { intent ->
                _state.value = PaymentFlowState(stage = PaymentStage.PROCESSING, intentId = intent.intentId)
                poll(intent.intentId)
            }
        }
    }

    fun startBill(billerId: String, reference: String, amountText: String, idemKey: String = UUID.randomUUID().toString()) {
        if (_state.value.stage == PaymentStage.PROCESSING || _state.value.stage == PaymentStage.CREATING) return
        viewModelScope.launch {
            _state.value = PaymentFlowState(stage = PaymentStage.CREATING)
            val amt = Amount(parseAmount(amountText), "LKR")
            val res = repo.createBillPayment(billerId, reference, amt, idemKey)
            res.onFailure { e ->
                _state.value = PaymentFlowState(stage = PaymentStage.FAILED, error = e.message ?: "error")
            }.onSuccess { intent ->
                _state.value = PaymentFlowState(stage = PaymentStage.PROCESSING, intentId = intent.intentId)
                poll(intent.intentId)
            }
        }
    }

    private suspend fun poll(id: String) {
        var wait = 1000L
        var elapsed = 0L
        val max = 120_000L
        while (elapsed <= max) {
            val r = repo.getPaymentIntent(id)
            val pi = r.getOrNull()
            if (pi != null) {
                if (
                    pi.returnUrl != null &&
                    _state.value.actionSentForId != id &&
                    (pi.status == PaymentStatus.PROCESSING || pi.status == PaymentStatus.PENDING)
                ) {
                    _state.value = _state.value.copy(actionUrl = pi.returnUrl, actionSentForId = id)
                }
                when (pi.status) {
                    PaymentStatus.SUCCESS -> {
                        _state.value = PaymentFlowState(stage = PaymentStage.SUCCEEDED, intentId = id, intent = pi)
                        return
                    }
                    PaymentStatus.FAILED -> {
                        _state.value = PaymentFlowState(stage = PaymentStage.FAILED, intentId = id, intent = pi, error = pi.description)
                        return
                    }
                    PaymentStatus.CANCELED -> {
                        _state.value = PaymentFlowState(stage = PaymentStage.CANCELED, intentId = id, intent = pi)
                        return
                    }
                    PaymentStatus.PENDING, PaymentStatus.PROCESSING -> {}
                }
            }
            delay(wait)
            elapsed += wait
            wait = (wait * 2).coerceAtMost(10_000L)
        }
        _state.value = PaymentFlowState(stage = PaymentStage.FAILED, intentId = id, error = "timeout")
    }

    fun clearActionUrl() {
        _state.value = _state.value.copy(actionUrl = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun parseAmount(text: String): Double {
        val cleaned = buildString {
            for (ch in text) if (ch.isDigit() || ch == '.' || ch == ',') append(ch)
        }.replace(",", "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}

data class PaymentFlowState(
    val stage: PaymentStage = PaymentStage.IDLE,
    val intentId: String? = null,
    val intent: PaymentIntent? = null,
    val error: String? = null,
    val actionUrl: String? = null,
    val actionSentForId: String? = null
)

enum class PaymentStage { IDLE, CREATING, PROCESSING, SUCCEEDED, FAILED, CANCELED }
