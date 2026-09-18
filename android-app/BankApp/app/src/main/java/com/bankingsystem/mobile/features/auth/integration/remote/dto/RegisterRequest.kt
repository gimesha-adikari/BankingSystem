package com.bankingsystem.mobile.features.auth.integration.remote.dto

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)