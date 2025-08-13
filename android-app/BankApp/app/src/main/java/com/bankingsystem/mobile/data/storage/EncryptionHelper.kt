package com.bankingsystem.mobile.data.storage

import android.util.Base64
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Helper object for encrypting and decrypting strings using AES/GCM/NoPadding.
 *
 * This object provides methods to encrypt and decrypt sensitive data, ensuring that
 * it is stored securely. It uses an Initialization Vector (IV) and a tag size
 * for GCM mode to enhance security.
 */
object EncryptionHelper {
    // Specifies the transformation for the Cipher object.
    // AES is the encryption algorithm.
    // GCM (Galois/Counter Mode) is a mode of operation for symmetric key cryptographic block ciphers
    // that has been widely adopted because of its efficiency and performance.
    // NoPadding means that the input data must be a multiple of the block size.
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    // Defines the size of the Initialization Vector (IV) in bytes.
    // For GCM, 12 bytes (96 bits) is a recommended size for IV.
    private const val IV_SIZE = 12
    // Defines the size of the authentication tag in bits.
    // For GCM, common tag sizes are 128, 120, 112, 104, or 96 bits.
    private const val TAG_SIZE = 128

    /**
     * Encrypts a plaintext string using the provided secret key.
     * @param plainText The string to be encrypted.
     * @param secretKey The secret key to use for encryption.
     * @return A Base64 encoded string representing the encrypted data (IV + ciphertext).
     */
    fun encrypt(plainText: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Prepend the IV to the ciphertext. The IV is needed for decryption.
        val byteBuffer = ByteBuffer.allocate(iv.size + encryptedBytes.size)
        byteBuffer.put(iv)
        byteBuffer.put(encryptedBytes)
        val cipherMessage = byteBuffer.array()

        return Base64.encodeToString(cipherMessage, Base64.NO_WRAP)
    }

    /**
     * Decrypts a Base64 encoded ciphertext string using the provided secret key.
     * @param cipherText The Base64 encoded string to be decrypted (IV + ciphertext).
     * @param secretKey The secret key to use for decryption.
     * @return The original plaintext string.
     */
    fun decrypt(cipherText: String, secretKey: SecretKey): String {
        val cipherMessage = Base64.decode(cipherText, Base64.NO_WRAP)

        // Extract the IV from the beginning of the cipher message.
        val byteBuffer = ByteBuffer.wrap(cipherMessage)
        val iv = ByteArray(IV_SIZE)
        byteBuffer.get(iv)
        val encryptedBytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(encryptedBytes)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        // Create GCMParameterSpec from the IV and tag size.
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
