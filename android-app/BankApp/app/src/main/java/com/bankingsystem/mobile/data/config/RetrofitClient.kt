package com.bankingsystem.mobile.data.config

import com.bankingsystem.mobile.BuildConfig
import com.bankingsystem.mobile.data.local.AuthStore
import com.bankingsystem.mobile.data.service.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit singleton with auth + logging interceptors.
 * IMPORTANT: Call init(authStore) before using apiService.
 */
object RetrofitClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL

    @Volatile
    private var _apiService: ApiService? = null
    val apiService: ApiService
        get() = _apiService ?: error("RetrofitClient not initialized. Call RetrofitClient.init(authStore) first.")

    fun isInitialized(): Boolean = _apiService != null
    fun init(authStore: AuthStore, onUnauthorized: () -> Unit = {}) {
        if (_apiService != null) return

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // set NONE in release if you prefer
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(authStore))
            .addInterceptor(AuthErrorInterceptor(onUnauthorized))
            .build()

        _apiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
