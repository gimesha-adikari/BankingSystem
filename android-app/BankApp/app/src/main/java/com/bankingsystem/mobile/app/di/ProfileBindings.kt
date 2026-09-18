package com.bankingsystem.mobile.app.di

import com.bankingsystem.mobile.features.profile.domain.repository.ProfileRepository
import com.bankingsystem.mobile.features.profile.integration.repository.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileBindings {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
