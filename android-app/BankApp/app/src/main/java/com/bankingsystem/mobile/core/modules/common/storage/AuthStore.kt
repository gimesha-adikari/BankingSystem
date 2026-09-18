package com.bankingsystem.mobile.core.modules.common.storage

interface AuthStore {
    suspend fun getToken(): String?
    suspend fun saveAuth(token: String, username: String, role: String)
}
