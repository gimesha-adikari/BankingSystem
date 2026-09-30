package com.bankingsystem.mobile.features.profile.integration.repository

import com.bankingsystem.mobile.features.profile.domain.model.UserProfile
import com.bankingsystem.mobile.features.profile.domain.repository.ProfileRepository
import com.bankingsystem.mobile.features.profile.integration.remote.api.ProfileApi
import com.bankingsystem.mobile.features.profile.integration.remote.dto.toDomain
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi
) : ProfileRepository {

    override suspend fun getMe(): Result<UserProfile> = runCatching { api.getMe().toDomain() }

    override suspend fun updateMe(payload: Map<String, String>): Result<Unit> = runCatching {
        val res = api.updateMe(payload)
        if (!res.isSuccessful) throw HttpException(res)
    }

    override suspend fun changePassword(current: String, new: String, confirm: String): Result<Unit> =
        runCatching {
            val body = mapOf(
                "currentPassword" to current,
                "newPassword" to new,
                "confirmPassword" to confirm
            )
            val res = api.changePassword(body)
            if (!res.isSuccessful) throw HttpException(res)
        }
}
