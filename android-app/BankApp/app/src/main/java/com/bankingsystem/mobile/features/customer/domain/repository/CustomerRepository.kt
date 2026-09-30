package com.bankingsystem.mobile.features.customer.domain.repository

import com.bankingsystem.mobile.features.customer.domain.model.CustomerRequest
import com.bankingsystem.mobile.features.customer.domain.model.CustomerResult

interface CustomerRepository {
    suspend fun upsertSelf(req: CustomerRequest): Result<CustomerResult>
}
