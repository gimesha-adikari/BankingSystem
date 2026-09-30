package com.bankingsystem.mobile.app.di

import com.bankingsystem.mobile.features.auth.domain.repository.UserRepository
import com.bankingsystem.mobile.features.auth.integration.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class AuthBindings {
    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
