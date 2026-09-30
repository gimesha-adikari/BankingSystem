package com.bankingsystem.mobile.features.accounts.interfaces.ui

import com.bankingsystem.mobile.features.accounts.domain.model.Account
import com.bankingsystem.mobile.features.accounts.interfaces.ui.models.*

fun toOpenRequestFields(
    type: AccountType,
    initialDeposit: String,
    branchId: String
): Triple<String, Double, String> {
    val amount = initialDeposit.toDoubleOrNull() ?: 0.0
    return Triple(type.name, amount, branchId)
}

private fun String.toAccountType(): AccountType = when (uppercase()) {
    "SAVINGS" -> AccountType.SAVINGS
    "CHECKING" -> AccountType.CHECKING
    "FIXED_DEPOSIT" -> AccountType.FIXED_DEPOSIT
    else -> AccountType.SAVINGS
}

private fun String.toAccountStatus(): AccountStatus = when (uppercase()) {
    "ACTIVE" -> AccountStatus.ACTIVE
    "FROZEN" -> AccountStatus.FROZEN
    "CLOSED" -> AccountStatus.CLOSED
    else -> AccountStatus.ACTIVE
}

fun Account.toUi(): AccountUi = AccountUi(
    accountId = accountId,
    accountNumber = accountNumber,
    type = accountType.toAccountType(),
    status = accountStatus.toAccountStatus(),
    balance = balance,
    branchName = branchName
)
