package com.bankingsystem.mobile.app.di

import com.bankingsystem.mobile.features.customer.domain.repository.CustomerRepository
import com.bankingsystem.mobile.features.customer.integration.repository.CustomerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CustomerBindings {
    @Binds
    @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository
}