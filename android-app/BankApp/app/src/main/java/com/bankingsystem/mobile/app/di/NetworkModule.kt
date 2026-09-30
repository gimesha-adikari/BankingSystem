package com.bankingsystem.mobile.app.di

import com.bankingsystem.mobile.BuildConfig
import com.bankingsystem.mobile.core.modules.common.storage.AuthStore
import com.bankingsystem.mobile.core.modules.common.storage.TokenManager
import com.bankingsystem.mobile.features.customer.integration.remote.api.CustomerApi
import com.bankingsystem.mobile.features.kyc.integration.remote.api.KycApi
import com.bankingsystem.mobile.features.wallet.integration.api.WalletApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @IODispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    class UnauthorizedHandler(
        private val tokenManager: TokenManager,
        private val io: CoroutineDispatcher
    ) {
        fun onUnauthorized() {
            CoroutineScope(io).launch {
                runCatching { tokenManager.clearToken() }
            }
        }
    }

    class AuthInterceptor(
        private val authStore: AuthStore,
        private val unauthorizedHandler: UnauthorizedHandler
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token: String? = runBlocking { authStore.getToken() }

            val req = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }.build()

            val resp = chain.proceed(req)
            if (resp.code == 401) {
                unauthorizedHandler.onUnauthorized()
            }
            return resp
        }
    }

    @Provides
    @Singleton
    fun provideUnauthorizedHandler(
        tokenManager: TokenManager,
        @IODispatcher io: CoroutineDispatcher
    ) = UnauthorizedHandler(tokenManager, io)

    @Provides
    @Singleton
    fun provideOkHttp(
        authStore: AuthStore,
        unauthorizedHandler: UnauthorizedHandler
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(AuthInterceptor(authStore, unauthorizedHandler))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BASIC
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()


    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        moshi: Moshi
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()

    @Provides
    fun provideAuthApi(retrofit: Retrofit): com.bankingsystem.mobile.features.auth.integration.remote.api.AuthApi =
        retrofit.create(com.bankingsystem.mobile.features.auth.integration.remote.api.AuthApi::class.java)

    @Provides
    fun provideAccountsApi(retrofit: Retrofit): com.bankingsystem.mobile.features.accounts.integration.remote.api.AccountsApi =
        retrofit.create(com.bankingsystem.mobile.features.accounts.integration.remote.api.AccountsApi::class.java)

    @Provides
    fun provideProfileApi(retrofit: Retrofit)
            : com.bankingsystem.mobile.features.profile.integration.remote.api.ProfileApi =
        retrofit.create(com.bankingsystem.mobile.features.profile.integration.remote.api.ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideKycApi(retrofit: Retrofit): KycApi =
        retrofit.create(KycApi::class.java)

    @Provides
    @Singleton
    fun provideCustomerApi(retrofit: Retrofit): CustomerApi =
        retrofit.create(CustomerApi::class.java)

    @Provides
    @Singleton
    fun provideWalletApi(retrofit: Retrofit): WalletApi =
        retrofit.create(WalletApi::class.java)
}
