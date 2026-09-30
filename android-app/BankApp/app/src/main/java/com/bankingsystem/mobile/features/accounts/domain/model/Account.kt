package com.bankingsystem.mobile.features.accounts.domain.model

data class Account(
    val accountId: String,
    val accountNumber: String,
    val accountType: String,
    val accountStatus: String,
    val balance: Double,
    val branchName: String? = null
)
