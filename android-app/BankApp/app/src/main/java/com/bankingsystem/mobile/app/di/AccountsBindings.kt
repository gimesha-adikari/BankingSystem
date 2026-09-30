package com.bankingsystem.mobile.app.di

import com.bankingsystem.mobile.features.accounts.domain.repository.AccountRepository
import com.bankingsystem.mobile.features.accounts.integration.repository.AccountRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class AccountsBindings {
    @Binds
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}
