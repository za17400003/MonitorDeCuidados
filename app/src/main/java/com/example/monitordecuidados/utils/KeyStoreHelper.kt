package com.example.monitordecuidados.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyStoreHelper {
    private const val KEY_ALIAS = "DatabasePassphraseKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "secure_prefs"
    private const val ENCRYPTED_PASSPHRASE_KEY = "enc_db_pass"
    private const val IV_KEY = "db_pass_iv"

    fun getDatabasePassphrase(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPass = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
        val ivBase64 = prefs.getString(IV_KEY, null)

        return try {
            if (encryptedPass != null && ivBase64 != null) {
                decrypt(encryptedPass, ivBase64)
            } else {
                generateAndSaveNewPassphrase(context)
            }
        } catch (e: Exception) {
            // If decryption fails (e.g. key invalidated), generate a new one
            generateAndSaveNewPassphrase(context)
        }
    }

    private fun generateAndSaveNewPassphrase(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newPass = generateRandomToken(32)
        val pair = encrypt(newPass)
        prefs.edit()
            .putString(ENCRYPTED_PASSPHRASE_KEY, pair.first)
            .putString(IV_KEY, pair.second)
            .apply()
        return newPass
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val entry = keyStore.getEntry(KEY_ALIAS, null)
        if (entry != null) {
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun encrypt(data: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Pair(
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    private fun decrypt(encryptedData: String, ivBase64: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decodedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun generateRandomToken(length: Int = 32): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
