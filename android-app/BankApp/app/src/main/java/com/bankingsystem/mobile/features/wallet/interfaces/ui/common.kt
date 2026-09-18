package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.compose.runtime.Immutable

@Immutable
data class WalletCardUi(
    val id: String,
    val brand: String,   // e.g., VISA, MASTERCARD
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val isDefault: Boolean = false,
)

@Immutable
data class QrPreview(
    val merchantName: String = "",
    val amount: String = "",
    val memo: String = ""
)

@Immutable
data class WalletUiState(
    val cards: List<WalletCardUi> = listOf(
        WalletCardUi(id = "1", brand = "VISA", last4 = "4242", expMonth = 12, expYear = 28, isDefault = true),
        WalletCardUi(id = "2", brand = "MASTERCARD", last4 = "5542", expMonth = 12, expYear = 28, isDefault = false)
    ),
    val isLoadingCards: Boolean = false,
    val lastError: String? = null,
    val qrPreview: QrPreview = QrPreview(),
    val selectedBillerId: String? = null
)
