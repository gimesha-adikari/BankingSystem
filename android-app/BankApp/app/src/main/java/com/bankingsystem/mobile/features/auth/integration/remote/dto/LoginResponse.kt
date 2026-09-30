package com.bankingsystem.mobile.features.auth.integration.remote.dto

data class LoginResponse(
    val token: String,
    val username: String,
    val role: String
)