package com.bankingsystem.mobile.app.di

import com.bankingsystem.mobile.BuildConfig
import com.bankingsystem.mobile.features.wallet.domain.repository.WalletRepository
import com.bankingsystem.mobile.features.wallet.integration.repository.FakeWalletRepository
import com.bankingsystem.mobile.features.wallet.integration.repository.WalletRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WalletBindings {

    @Provides
    @Singleton
    fun provideWalletRepository(
        real: WalletRepositoryImpl,
        fake: FakeWalletRepository
    ): WalletRepository = if (!BuildConfig.DEBUG) fake else real
}
