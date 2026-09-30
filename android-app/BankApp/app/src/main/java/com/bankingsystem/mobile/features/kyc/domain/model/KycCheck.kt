package com.bankingsystem.mobile.features.kyc.domain.model

data class KycCheck(
    val type: String,
    val score: Double? = null,
    val passed: Boolean? = null
)