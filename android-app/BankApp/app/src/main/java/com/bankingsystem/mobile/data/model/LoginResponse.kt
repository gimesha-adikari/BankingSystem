package com.bankingsystem.mobile.data.model

data class LoginResponse(
    val token: String,
    val username: String,
    val role: String
)
