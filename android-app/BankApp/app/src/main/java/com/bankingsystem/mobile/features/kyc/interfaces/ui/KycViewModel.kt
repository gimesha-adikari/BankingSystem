package com.bankingsystem.mobile.features.kyc.interfaces.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.features.kyc.domain.model.KycCaseStatus
import com.bankingsystem.mobile.features.kyc.domain.model.KycCheck
import com.bankingsystem.mobile.features.kyc.domain.model.KycUploadIds
import com.bankingsystem.mobile.features.kyc.domain.model.KycUploadType
import com.bankingsystem.mobile.features.kyc.domain.repository.KycRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KycViewModel @Inject constructor(
    private val repo: KycRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(KycUiState())
    val ui: StateFlow<KycUiState> = _ui.asStateFlow()

    private val _uploadedIds = MutableStateFlow<Map<KycUploadType, String>>(emptyMap())
    val uploadedIds: StateFlow<Map<KycUploadType, String>> = _uploadedIds.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _status = MutableStateFlow<KycCaseStatus?>(null)
    val status: StateFlow<KycCaseStatus?> = _status.asStateFlow()

    private val _checks = MutableStateFlow<List<KycCheck>>(emptyList())
    val checks: StateFlow<List<KycCheck>> = _checks.asStateFlow()

    fun refreshStatusOnce() = viewModelScope.launch {
        val st = runCatching { repo.myCase() }.getOrNull()
        _status.value = st
        if (st != null) _checks.value = runCatching { repo.myChecks(st.caseId) }.getOrElse { emptyList() }
    }

    private var pollJob: Job? = null

    fun startPollingStatus(intervalMs: Long = 3000L) {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                val st = runCatching { repo.myCase() }.getOrNull()
                _status.value = st
                if (st != null) {
                    val ch = runCatching { repo.myChecks(st.caseId) }.getOrElse { emptyList() }
                    _checks.value = ch
                }

                val s = _status.value?.status
                if (s == "APPROVED" || s == "REJECTED" || s == "NEEDS_MORE_INFO") break
                delay(intervalMs)
            }
        }
    }

    fun stopPollingStatus() {
        pollJob?.cancel()
        pollJob = null
    }

    fun pollStatusUntilTerminal(intervalMs: Long = 3000L) = startPollingStatus(intervalMs)


    fun go(step: KycStep) = _ui.update { it.copy(step = step) }

    fun next() = _ui.update {
        val s = when (it.step) {
            KycStep.Document -> KycStep.Selfie
            KycStep.Selfie -> KycStep.Address
            KycStep.Address -> KycStep.Review
            KycStep.Review -> KycStep.Review
        }
        it.copy(step = s)
    }

    fun back() = _ui.update {
        val s = when (it.step) {
            KycStep.Document -> KycStep.Document
            KycStep.Selfie -> KycStep.Document
            KycStep.Address -> KycStep.Selfie
            KycStep.Review -> KycStep.Address
        }
        it.copy(step = s)
    }

    fun setDocFront(uri: Uri?) { _ui.update { it.copy(docFront = uri) }; if (uri != null) upload(KycUploadType.DOC_FRONT, uri) }
    fun setDocBack(uri: Uri?)  { _ui.update { it.copy(docBack = uri)  }; if (uri != null) upload(KycUploadType.DOC_BACK, uri)  }
    fun setSelfie(uri: Uri?)   { _ui.update { it.copy(selfie = uri)   }; if (uri != null) upload(KycUploadType.SELFIE, uri)    }
    fun setAddressProof(uri: Uri?) { _ui.update { it.copy(addressProof = uri) }; if (uri != null) upload(KycUploadType.ADDRESS_PROOF, uri) }

    fun setDocQuality(q: DocQuality) = _ui.update { it.copy(docQuality = q) }
    fun setOcrFields(fields: List<OcrField>) = _ui.update { it.copy(ocrFields = fields) }
    fun setLiveness(score: Float?) = _ui.update { it.copy(livenessScore = score) }
    fun setFaceMatch(score: Float?) = _ui.update { it.copy(faceMatchScore = score) }
    fun setConsent(accepted: Boolean) = _ui.update { it.copy(consentAccepted = accepted) }

    private fun upload(type: KycUploadType, uri: Uri) {
        viewModelScope.launch {
            _uploading.value = true
            runCatching { repo.upload(uri, type) }
                .onSuccess { part -> _uploadedIds.update { it + (type to part.id) } }
            _uploading.value = false
        }
    }

    fun canContinueFromDocument(): Boolean {
        val ok = ui.value.docFront != null && ui.value.docBack != null
        val q = ui.value.docQuality
        val blurOk = (q.blurScore ?: 1f) >= 0.10f
        val glareOk = (q.glareScore ?: 1f) >= 0.60f
        val cornersOk = (q.cornerCoverage ?: 0) >= 3
        return ok && blurOk && glareOk && cornersOk
    }

    fun canContinueFromSelfie(): Boolean {
        val liveOk = (ui.value.livenessScore ?: 0f) >= 0.60f
        val matchOk = (ui.value.faceMatchScore ?: 0f) >= 0.60f
        return ui.value.selfie != null && liveOk && matchOk
    }

    fun canContinueFromAddress(): Boolean = ui.value.addressProof != null
    fun canSubmit(): Boolean = ui.value.consentAccepted

    fun docsOk() = ui.value.docFront != null && ui.value.docBack != null
    fun selfieOk(): Boolean {
        val liveOk = (ui.value.livenessScore ?: 1f) >= 0.60f
        val matchOk = (ui.value.faceMatchScore ?: 1f) >= 0.60f
        return ui.value.selfie != null && liveOk && matchOk
    }
    fun addressOk() = ui.value.addressProof != null

    fun readyToSubmit(strict: Boolean = true): Boolean =
        if (strict) {
            docsOk() && selfieOk() && addressOk() && ui.value.consentAccepted
        } else {
            listOf(docsOk(), selfieOk(), addressOk()).count { it } >= 2 && ui.value.consentAccepted
        }

    suspend fun submit(): Boolean {
        Log.d("KYC", "submit(): invoked")

        if (!readyToSubmit(strict = true)) {
            Log.w("KYC", "submit(): not ready.")
            return false
        }
        val idsMap = uploadedIds.value
        val missing = KycUploadType.entries.filterNot { it in idsMap }
        if (missing.isNotEmpty()) {
            Log.w("KYC", "submit(): missing upload ids: $missing")
            return false
        }

        return try {
            val ids = KycUploadIds(
                docFrontId = idsMap.getValue(KycUploadType.DOC_FRONT),
                docBackId  = idsMap.getValue(KycUploadType.DOC_BACK),
                selfieId   = idsMap.getValue(KycUploadType.SELFIE),
                addressId  = idsMap.getValue(KycUploadType.ADDRESS_PROOF)
            )
            val result = repo.submit(consent = ui.value.consentAccepted, ids = ids)
            val okStatuses = setOf("PENDING","AUTO_REVIEW","UNDER_REVIEW","APPROVED","REJECTED","NEEDS_MORE_INFO")
            okStatuses.contains(result.status.trim().uppercase())
        } catch (e: Exception) {
            Log.e("KYC", "submit(): exception", e)
            false
        }
    }



}
