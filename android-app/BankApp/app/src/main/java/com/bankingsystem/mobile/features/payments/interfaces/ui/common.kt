package com.bankingsystem.mobile.features.payments.interfaces.ui

import androidx.compose.runtime.Immutable

enum class PaymentKind { NONE, TRANSFER, DEPOSIT, WITHDRAW }

@Immutable
data class PaymentDraft(
    val kind: PaymentKind = PaymentKind.NONE,
    val sourceAccount: String = "",
    val destinationAccount: String = "",
    val destinationBank: String = "",
    val amount: String = "",
    val description: String = "",
)
