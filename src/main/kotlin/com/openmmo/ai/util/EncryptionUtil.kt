package com.openmmo.ai.util

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object EncryptionUtil {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    fun encrypt(plaintext: String, keyBase64: String): String {
        val key = Base64.getDecoder().decode(keyBase64)
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, 0, key.size, "AES")

        // Generate random IV
        val iv = ByteArray(IV_LENGTH)
        Random.nextBytes(iv)

        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray())

        // Combine IV + ciphertext
        val combined = ByteArray(IV_LENGTH + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH)
        System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encrypted: String, keyBase64: String): String {
        val key = Base64.getDecoder().decode(keyBase64)
        val combined = Base64.getDecoder().decode(encrypted)

        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH)

        val ciphertext = ByteArray(combined.size - IV_LENGTH)
        System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, 0, key.size, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext)
    }
}

