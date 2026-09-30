package com.bankingsystem.mobile.features.kyc.interfaces.ui

import android.net.Uri

enum class KycStep { Document, Selfie, Address, Review }

data class DocQuality(
    val blurScore: Float? = null,
    val glareScore: Float? = null,
    val cornerCoverage: Int? = null
)

data class OcrField(
    val label: String,
    val value: String,
    val confidence: Float? = null
)

data class KycUiState(
    val step: KycStep = KycStep.Document,
    val docFront: Uri? = null,
    val docBack: Uri? = null,
    val docQuality: DocQuality = DocQuality(),
    val ocrFields: List<OcrField> = emptyList(),
    val selfie: Uri? = null,
    val livenessScore: Float? = null,
    val faceMatchScore: Float? = null,
    val addressProof: Uri? = null,
    val consentAccepted: Boolean = false
)
