package com.bankingsystem.mobile.features.kyc.domain.model

/** Backend wire values accepted by KycUploadController. */
enum class KycUploadType(val wireValue: String) {
    DOC_FRONT("DOC_FRONT"),
    DOC_BACK("DOC_BACK"),
    SELFIE("SELFIE"),
    ADDRESS_PROOF("ADDRESS_PROOF")
}
