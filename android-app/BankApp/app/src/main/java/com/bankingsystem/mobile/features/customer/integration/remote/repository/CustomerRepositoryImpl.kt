package com.bankingsystem.mobile.features.customer.integration.repository

import com.bankingsystem.mobile.features.customer.domain.model.CustomerRequest
import com.bankingsystem.mobile.features.customer.domain.model.CustomerResult
import com.bankingsystem.mobile.features.customer.domain.repository.CustomerRepository
import com.bankingsystem.mobile.features.customer.integration.remote.api.CustomerApi
import com.bankingsystem.mobile.features.customer.integration.remote.dto.toBody
import com.bankingsystem.mobile.features.profile.domain.repository.ProfileRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val api: CustomerApi,
    private val profileRepo: ProfileRepository
) : CustomerRepository {

    override suspend fun upsertSelf(req: CustomerRequest): Result<CustomerResult> = runCatching {
        // 1) Always include userId (backend validates @NotNull, even on /customers/me)
        val userId = req.userId ?: profileRepo.getMe().getOrThrow().userId
        val withUid = req.copy(userId = userId)

        // 2) Try self endpoint first
        val self = api.upsertMe(withUid.toBody())
        when {
            self.isSuccessful -> {
                val dto = self.body()
                return@runCatching CustomerResult(id = dto?.id, status = dto?.status)
            }
            self.code() == 409 -> { // KYC not approved
                throw IllegalStateException(
                    "Your identity verification must be approved before creating a customer profile."
                )
            }
            self.code() in setOf(404, 405, 501) -> {
                // 3) Backend without /me: fall back to staff POST (requires staff token)
                val fb = api.create(withUid.toBody())
                if (!fb.isSuccessful) throw HttpException(fb)
                val dto = fb.body()
                CustomerResult(id = dto?.id, status = dto?.status)
            }
            else -> throw HttpException(self) // e.g., 400 From validation, 403 forbidden, etc.
        }
    }
}
