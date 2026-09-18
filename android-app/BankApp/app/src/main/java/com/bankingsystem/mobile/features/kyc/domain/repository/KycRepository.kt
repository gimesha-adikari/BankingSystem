package com.bankingsystem.mobile.features.kyc.domain.repository

import android.net.Uri
import com.bankingsystem.mobile.features.kyc.domain.model.*

interface KycRepository {
    suspend fun upload(uri: Uri, type: KycUploadType): UploadedPart
    suspend fun submit(consent: Boolean, ids: KycUploadIds): KycSubmitResponse
    suspend fun myCase(): KycCaseStatus
    suspend fun myChecks(caseId: String): List<KycCheck>
}
