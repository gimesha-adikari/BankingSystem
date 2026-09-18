package com.bankingsystem.mobile

import com.bankingsystem.mobile.features.auth.integration.remote.dto.ForgotPasswordRequest
import com.bankingsystem.mobile.features.kyc.domain.model.KycUploadType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiContractTest {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun forgotPasswordSerializesTheBackendRequestBody() {
        val json = moshi.adapter(ForgotPasswordRequest::class.java)
            .toJson(ForgotPasswordRequest("customer@example.test"))

        assertEquals("{\"email\":\"customer@example.test\"}", json)
    }

    @Test
    fun kycUploadTypesUseBackendWireValues() {
        assertEquals(
            listOf("DOC_FRONT", "DOC_BACK", "SELFIE", "ADDRESS_PROOF"),
            KycUploadType.entries.map { it.wireValue }
        )
    }
}
