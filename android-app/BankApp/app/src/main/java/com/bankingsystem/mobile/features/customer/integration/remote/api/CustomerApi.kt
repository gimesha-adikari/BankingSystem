package com.bankingsystem.mobile.features.customer.integration.remote.api

import com.bankingsystem.mobile.features.customer.integration.remote.dto.CustomerRequestBody
import com.bankingsystem.mobile.features.customer.integration.remote.dto.CustomerResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface CustomerApi {

    @PUT("api/v1/customers/me")
    suspend fun upsertMe(@Body body: CustomerRequestBody): Response<CustomerResponseDto>

    @POST("api/v1/customers")
    suspend fun create(@Body body: CustomerRequestBody): Response<CustomerResponseDto>
}
