package com.bankingsystem.mobile.features.accounts.domain.model

data class Transaction(
    val transactionId: String,
    val accountId: String,
    val type: String,
    val amount: Double,
    val balanceAfter: Double,
    val description: String?,
    val createdAt: String?
)
