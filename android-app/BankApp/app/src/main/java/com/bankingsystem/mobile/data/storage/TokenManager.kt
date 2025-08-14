package com.bankingsystem.mobile.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.crypto.SecretKey

private val Context.dataStore by preferencesDataStore(name = "secure_prefs")

class TokenManager(private val context: Context) {

    private val TOKEN_KEY = stringPreferencesKey("encrypted_token")
    private val secretKey: SecretKey by lazy { KeyStoreManager.getSecretKey() }

    suspend fun saveToken(token: String) {
        val encryptedToken = EncryptionHelper.encrypt(token, secretKey)
        context.dataStore.edit { prefs -> prefs[TOKEN_KEY] = encryptedToken }
    }

    suspend fun getToken(): String? {
        val prefs = context.dataStore.data.first()
        val encrypted = prefs[TOKEN_KEY] ?: return null
        return try {
            EncryptionHelper.decrypt(encrypted, secretKey)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]?.let { encrypted ->
            try {
                EncryptionHelper.decrypt(encrypted, secretKey)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs -> prefs.remove(TOKEN_KEY) }
    }
}
