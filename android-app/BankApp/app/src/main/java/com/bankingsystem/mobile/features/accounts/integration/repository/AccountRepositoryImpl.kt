package com.bankingsystem.mobile.features.accounts.integration.repository

import com.bankingsystem.mobile.features.accounts.domain.errors.ApiCallException
import com.bankingsystem.mobile.features.accounts.domain.errors.CustomerMissingException
import com.bankingsystem.mobile.features.accounts.domain.model.*
import com.bankingsystem.mobile.features.accounts.domain.repository.AccountRepository
import com.bankingsystem.mobile.features.accounts.integration.remote.api.AccountsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val api: AccountsApi
) : AccountRepository {

    override suspend fun getBranches(): List<Branch> = ioWrap {
        api.getBranches().map { Branch(it.branchId.toString(), it.branchName) }
    }

    override suspend fun openAccount(
        accountType: String,
        initialDeposit: Double,
        branchId: String
    ): Account = ioWrap {
        val res = api.openAccount(
            com.bankingsystem.mobile.features.accounts.integration.remote.dto.AccountOpenRequest(
                accountType = accountType,
                initialDeposit = initialDeposit,
                branchId = branchId.toIntOrNull()
                    ?: throw ApiCallException("Invalid branch ID: $branchId")
            )
        )
        Account(
            accountId = res.accountId,
            accountNumber = res.accountNumber,
            accountType = res.accountType,
            accountStatus = res.accountStatus,
            balance = res.balance
        )
    }

    override suspend fun getAccountTransactions(accountId: String): List<Transaction> = ioWrap {
        api.getAccountTransactions(accountId).map {
            Transaction(
                transactionId = it.transactionId,
                accountId = it.accountId,
                type = it.type,
                amount = it.amount,
                balanceAfter = it.balanceAfter,
                description = it.description,
                createdAt = it.createdAt
            )
        }
    }

    override suspend fun getMyAccounts(): List<Account> = ioWrap {
        api.getMyAccounts().map {
            Account(
                accountId = it.accountId,
                accountNumber = it.accountNumber,
                accountType = it.accountType,
                accountStatus = it.accountStatus,
                balance = it.balance,
                branchName = it.branchName
            )
        }
    }

    private suspend inline fun <T> ioWrap(crossinline block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: HttpException) {
                val code = e.code()
                val raw = e.response()?.errorBody()?.string().orEmpty()
                val msg = runCatching { JSONObject(raw).optString("message") }.getOrNull().orEmpty()

                if (code == 404 && msg.contains("Customer not found", true)) {
                    throw CustomerMissingException(
                        "We couldn’t find your customer profile yet. Please create it to view or open accounts."
                    )
                }
                throw ApiCallException(msg.ifBlank { "Network error ($code)" })
            } catch (e: Exception) {
                throw ApiCallException(e.message ?: "Unexpected error")
            }
        }
}
