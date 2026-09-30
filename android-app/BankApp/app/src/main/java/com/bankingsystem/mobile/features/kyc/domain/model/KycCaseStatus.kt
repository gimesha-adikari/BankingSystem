package com.bankingsystem.mobile.features.kyc.domain.model

data class KycCaseStatus(
    val caseId: String,
    val status: String,
    val decisionReason: String? = null
)