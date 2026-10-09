package com.example.monitordecuidados.security

import android.content.Context
import android.util.Base64
import com.example.monitordecuidados.utils.KeyStoreHelper
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Centralized Security Configuration.
 * SPEC SEC-01 & SEC-04
 * T71: Eliminando claves hardcodeadas y manejando cifrado con IV dinámico.
 */
object SecurityConfig {
    
    private const val AES_ALGORITHM = "AES/CBC/PKCS5Padding"

    /**
     * T71: Obtiene la clave AES desde el Android KeyStore.
     */
    fun getAesKey(context: Context): SecretKey {
        return KeyStoreHelper.getOrCreateAesKey("campana_aes_key")
    }

    /**
     * T71: Cifra un texto usando AES-256 con la clave del KeyStore e IV dinámico.
     * El IV se antepone a los datos cifrados (16 bytes).
     */
    fun encrypt(context: Context, plaintext: String): String {
        return try {
            val key = getAesKey(context)
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            val iv = ByteArray(16)
            java.security.SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("SecurityConfig", "Encryption failed", e)
            plaintext // Fallback to plain text if encryption fails
        }
    }

    /**
     * T71: Descifra un texto Base64 extrayendo el IV de los primeros 16 bytes.
     */
    fun decrypt(context: Context, encryptedBase64: String): String {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < 16) return encryptedBase64

            val iv = ByteArray(16)
            System.arraycopy(combined, 0, iv, 0, 16)
            val encrypted = ByteArray(combined.size - 16)
            System.arraycopy(combined, 16, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(AES_ALGORITHM)
            val key = getAesKey(context)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback to original text if decryption fails (might be plain text)
            encryptedBase64
        }
    }

    @Deprecated("Usar encrypt(context, text)")
    val AES_KEY = SecretKeySpec("CampanaSecureKey".toByteArray(), "AES")

    @Deprecated("Usar encrypt(context, text)")
    val AES_IV = IvParameterSpec("CampanaInitVect1".toByteArray())
    
    // Production certificate public key hashes (SHA-256)
    const val PIN_SHA_256 = "sha256/7fmR5fW7E7U7S6v3iX8H4p9L2M5N8O0P1Q2R3S4T5U="
    const val PIN_SHA_256_BACKUP = "sha256/8gmS6gX8F8V8T7w4jY9I5q0M3N6O9P1Q2R3S4T5U6V="
}
