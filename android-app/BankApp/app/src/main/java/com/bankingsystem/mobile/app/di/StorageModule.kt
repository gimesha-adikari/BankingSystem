// di/StorageModule.kt
package com.bankingsystem.mobile.app.di

import android.content.Context
import com.bankingsystem.mobile.core.modules.common.storage.AuthStore
import com.bankingsystem.mobile.core.modules.common.storage.AuthStoreImpl
import com.bankingsystem.mobile.core.modules.common.storage.DefaultAccountStore
import com.bankingsystem.mobile.core.modules.common.storage.DefaultAccountStoreImpl
import com.bankingsystem.mobile.core.modules.common.storage.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides @Singleton
    fun provideTokenManager(@ApplicationContext ctx: Context): TokenManager =
        TokenManager(ctx)

    @Provides @Singleton
    fun provideAuthStore(tokenManager: TokenManager): AuthStore =
        AuthStoreImpl(tokenManager)

    @Provides @Singleton
    fun provideDefaultAccountStore(@ApplicationContext ctx: Context): DefaultAccountStore =
        DefaultAccountStoreImpl(ctx)
}
