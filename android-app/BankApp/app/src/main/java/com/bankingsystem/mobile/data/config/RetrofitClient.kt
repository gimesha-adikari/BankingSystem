package com.bankingsystem.mobile.data.config

import com.bankingsystem.mobile.data.service.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object for Retrofit client configuration.
 */
object RetrofitClient {
    // Base URL for the API. Using 10.0.2.2 for Android emulator to connect to localhost.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // HTTP logging interceptor for debugging network requests and responses.
    // Logs the body of the requests and responses.
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient with the logging interceptor.
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    /**
     * Lazily initialized Retrofit instance for [ApiService].
     * This ensures that the Retrofit instance is created only when it's first accessed.
     */
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
