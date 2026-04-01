package com.example.monitordecuidados.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Helper class for AES-256 encryption.
 * SRS Requirement: All custom phrases must be encrypted using AES-256 before syncing.
 */
object AES256EncryptionHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 32 // 256 bits

    fun encrypt(plaintext: String, keyDerivationInput: String): String {
        try {
            val key = deriveKey(keyDerivationInput)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun decrypt(encryptedBase64: String, keyDerivationInput: String): String {
        try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = ByteArray(16)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            val encrypted = ByteArray(combined.size - iv.size)
            System.arraycopy(combined, iv.size, encrypted, 0, encrypted.size)
            
            val key = deriveKey(keyDerivationInput)
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            val decrypted = cipher.doFinal(encrypted)
            
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun deriveKey(input: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }
}
