package com.bankingsystem.mobile.features.profile.domain.repository

import com.bankingsystem.mobile.features.profile.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getMe(): Result<UserProfile>
    suspend fun updateMe(payload: Map<String, String>): Result<Unit>
    suspend fun changePassword(current: String, new: String, confirm: String): Result<Unit>
}
