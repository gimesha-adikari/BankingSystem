package com.bankingsystem.mobile.features.accounts.integration.remote.api

import com.bankingsystem.mobile.features.accounts.integration.remote.dto.*
import retrofit2.http.*

interface AccountsApi {
    @GET("api/v1/branches")
    suspend fun getBranches(): List<BranchNet>

    @POST("api/v1/accounts")
    suspend fun openAccount(@Body body: AccountOpenRequest): AccountResponseNet

    @GET("api/v1/accounts/{id}/transactions")
    suspend fun getAccountTransactions(@Path("id") accountId: String): List<TransactionNet>

    @GET("api/v1/accounts/my")
    suspend fun getMyAccounts(): List<AccountNet>
}
