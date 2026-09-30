package com.bankingsystem.mobile.features.kyc.integration.remote.dto

data class KycCaseStatusResponse(
    val caseId: String,
    val status: String,
    val decisionReason: String? = null
)