package com.bankingsystem.mobile.features.auth.domain.model

data class ValidateTokenResponse(
    val username: String,
    val role: String
)