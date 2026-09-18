package com.bankingsystem.mobile.features.customer.integration.remote.dto

import com.bankingsystem.mobile.features.customer.domain.model.CustomerRequest

data class CustomerRequestBody(
    val firstName: String,
    val lastName: String,
    val gender: String,
    val email: String,
    val phone: String,
    val address: String?,
    val dateOfBirth: String,
    val status: String,
    val userId: String?
)

fun CustomerRequest.toBody() = CustomerRequestBody(
    firstName, lastName, gender, email, phone, address, dateOfBirth, status, userId
)

data class CustomerResponseDto(
    val id: String? = null,
    val status: String? = null
)
