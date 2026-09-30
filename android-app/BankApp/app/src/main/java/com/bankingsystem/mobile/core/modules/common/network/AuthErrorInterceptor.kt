package com.bankingsystem.mobile.core.modules.common.network

import okhttp3.Interceptor
import okhttp3.Response
class AuthErrorInterceptor(
    private val onUnauthorized: () -> Unit = {}
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            onUnauthorized()
        }
        return response
    }
}
