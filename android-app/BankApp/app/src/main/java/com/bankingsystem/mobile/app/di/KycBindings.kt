package com.bankingsystem.mobile.app.di

import com.bankingsystem.mobile.features.kyc.domain.repository.KycRepository
import com.bankingsystem.mobile.features.kyc.integration.repository.KycRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class KycBindings {
    @Binds
    abstract fun bindKycRepository(impl: KycRepositoryImpl): KycRepository
}
