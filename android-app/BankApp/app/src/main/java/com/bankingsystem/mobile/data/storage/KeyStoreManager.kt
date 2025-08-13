package com.bankingsystem.mobile.data.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages the generation and retrieval of a secret key from the Android Keystore.
 * This key is used for encrypting and decrypting sensitive data, such as authentication tokens.
 */
object KeyStoreManager {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "token_encryption_key"

    /**
     * Retrieves the secret key from the Android Keystore.
     * If the key does not exist, it generates a new one.
     *
     * @return The [SecretKey] used for encryption and decryption.
     */
    fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey // Attempt to retrieve an existing key
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply { // Configure key generation parameters
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
            setUserAuthenticationRequired(false)  // Set to true if biometric authentication is required for key usage
        }.build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}
