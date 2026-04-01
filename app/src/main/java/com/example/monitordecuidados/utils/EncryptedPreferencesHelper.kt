package com.example.monitordecuidados.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptedPreferencesHelper {
    private const val PREFS_NAME = "monitordecuidados_prefs_encrypted"

    fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveString(context: Context, key: String, value: String?) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getPrefs(context).getString(key, defaultValue)
    }

    fun saveBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getPrefs(context).getBoolean(key, defaultValue)
    }

    fun saveInt(context: Context, key: String, value: Int) {
        getPrefs(context).edit().putInt(key, value).apply()
    }

    fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return getPrefs(context).getInt(key, defaultValue)
    }

    fun saveFloat(context: Context, key: String, value: Float) {
        getPrefs(context).edit().putFloat(key, value).apply()
    }

    fun getFloat(context: Context, key: String, defaultValue: Float = 0f): Float {
        return getPrefs(context).getFloat(key, defaultValue)
    }
    
    fun remove(context: Context, key: String) {
        getPrefs(context).edit().remove(key).apply()
    }
}
