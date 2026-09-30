package com.bankingsystem.mobile.core.modules.common.security

import android.util.Base64
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptionHelper {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    fun encrypt(plainText: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val byteBuffer = ByteBuffer.allocate(iv.size + encryptedBytes.size)
        byteBuffer.put(iv)
        byteBuffer.put(encryptedBytes)
        val cipherMessage = byteBuffer.array()

        return Base64.encodeToString(cipherMessage, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, secretKey: SecretKey): String {
        val cipherMessage = Base64.decode(cipherText, Base64.NO_WRAP)

        val byteBuffer = ByteBuffer.wrap(cipherMessage)
        val iv = ByteArray(IV_SIZE)
        byteBuffer.get(iv)
        val encryptedBytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(encryptedBytes)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
