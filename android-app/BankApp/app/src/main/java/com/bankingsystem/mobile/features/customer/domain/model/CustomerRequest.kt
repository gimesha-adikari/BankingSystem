package com.bankingsystem.mobile.features.customer.domain.model

data class CustomerRequest(
    val firstName: String,
    val lastName: String,
    val gender: String,
    val email: String,
    val phone: String,
    val address: String?,
    val dateOfBirth: String,
    val status: String = "ACTIVE",
    val userId: String? = null
)

data class CustomerResult(
    val id: String?,
    val status: String?
)
