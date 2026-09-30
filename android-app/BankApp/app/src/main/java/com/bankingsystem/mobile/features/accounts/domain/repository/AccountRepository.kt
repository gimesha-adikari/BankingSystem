package com.bankingsystem.mobile.features.accounts.domain.repository

import com.bankingsystem.mobile.features.accounts.domain.model.*

interface AccountRepository {
    suspend fun getBranches(): List<Branch>
    suspend fun openAccount(
        accountType: String,
        initialDeposit: Double,
        branchId: String
    ): Account
    suspend fun getAccountTransactions(accountId: String): List<Transaction>
    suspend fun getMyAccounts(): List<Account>
}
