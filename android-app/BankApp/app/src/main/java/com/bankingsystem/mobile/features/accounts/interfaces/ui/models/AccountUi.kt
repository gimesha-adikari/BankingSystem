package com.bankingsystem.mobile.features.accounts.interfaces.ui.models

enum class AccountStatus { ACTIVE, FROZEN, CLOSED }
enum class AccountType { SAVINGS, CHECKING, FIXED_DEPOSIT }

data class AccountUi(
    val accountId: String,
    val accountNumber: String,
    val type: AccountType,
    val status: AccountStatus,
    val balance: Double,
    val branchName: String? = null
)
data class BranchOption(val id: String, val name: String)
