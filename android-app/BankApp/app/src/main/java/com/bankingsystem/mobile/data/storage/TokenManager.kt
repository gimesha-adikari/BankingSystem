package com.bankingsystem.mobile.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.crypto.SecretKey

// Defines an extension property for Context to access DataStore.
// "secure_prefs" is the name of the preferences file.
private val Context.dataStore by preferencesDataStore(name = "secure_prefs")

/**
 * Manages the storage and retrieval of authentication tokens.
 * It uses DataStore for persistence and Android KeyStore for encryption.
 */
class TokenManager(private val context: Context) {

    // Defines a key for storing the encrypted token in DataStore.
    private val TOKEN_KEY = stringPreferencesKey("encrypted_token")
    // Lazily initializes the secret key from KeyStoreManager.
    private val secretKey: SecretKey by lazy { KeyStoreManager.getSecretKey() }

    /**
     * Saves the provided token after encrypting it.
     * @param token The token string to be saved.
     */
    suspend fun saveToken(token: String) {
        // Encrypts the token using the secret key.
        val encryptedToken = EncryptionHelper.encrypt(token, secretKey)
        // Saves the encrypted token to DataStore.
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = encryptedToken
        }
    }
    // A Flow that emits the decrypted token whenever it changes in DataStore.
    // It returns null if the token is not found or decryption fails.
    val tokenFlow: Flow<String?> = context.dataStore.data
        .map { prefs ->
            prefs[TOKEN_KEY]?.let { encryptedToken ->
                try {
                    EncryptionHelper.decrypt(encryptedToken, secretKey)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Returns null if decryption fails.
                    null
                }
            }
        }

    /**
     * Clears the stored token from DataStore.
     */
    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
        }
    }
}
