package com.bankingsystem.mobile.features.kyc.integration.remote.dto

data class KycCheckDto(
    val type: String,
    val score: Double? = null,
    val passed: Boolean? = null
)