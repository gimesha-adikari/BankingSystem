package com.bankingsystem.mobile.features.kyc.integration.remote.dto

data class KycSubmitBody(
    val docFrontId: String,
    val docBackId: String,
    val selfieId: String,
    val addressId: String,
    val consent: Boolean
)