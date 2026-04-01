package com.example.monitordecuidados.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

/**
 * Singleton that loads string translations from Firestore real-time database.
 * Phase 6 - Gap 1: Dynamic Localization.
 */
object StringsLocalizationManager {
    private const val TAG = "StringsLocMgr"
    private val cache = mutableMapOf<String, String>()
    private var currentLanguage = Locale.getDefault().language

    fun init(context: Context) {
        loadTranslations(currentLanguage)
    }

    fun loadTranslations(language: String) {
        currentLanguage = language
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("localization").document(language)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data
                        data?.forEach { (key, value) ->
                            cache[key] = value.toString()
                        }
                        Log.d(TAG, "Translations updated for $language")
                    } else {
                        Log.d(TAG, "No translations found for $language")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore for localization", e)
        }
    }

    fun getString(context: Context, key: String, defaultResId: Int): String {
        return cache[key] ?: context.getString(defaultResId)
    }

    fun getCurrentLanguage(): String = currentLanguage
}
