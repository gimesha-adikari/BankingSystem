package com.bankingsystem.mobile.core.modules.common.network

import com.bankingsystem.mobile.core.modules.common.storage.AuthStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val store: AuthStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (original.header("Authorization") != null) {
            return chain.proceed(original)
        }

        val token = runBlocking { store.getToken() }
        val req = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(req)
    }
}
