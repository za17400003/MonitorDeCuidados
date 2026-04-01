package com.example.monitordecuidados.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.monitordecuidados.cloud.FirebaseService
import com.example.monitordecuidados.models.UserPreferences
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper

class PreferencesViewModel : ViewModel() {
    private val _preferencesState = MutableLiveData<UserPreferences>()
    val preferencesState: LiveData<UserPreferences> = _preferencesState

    private val db = FirebaseService.db
    private val auth = FirebaseService.auth

    fun getPreferences() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val prefs = UserPreferences(
                        autoAcceptCalls = document.getBoolean("autoAcceptCalls") ?: false,
                        enableShakeDetection = document.getBoolean("enableShakeDetection") ?: true,
                        notificationSound = document.getBoolean("notificationSound") ?: true
                    )
                    _preferencesState.value = prefs
                }
            }
    }

    fun updateAutoAcceptCalls(value: Boolean) {
        updatePreference("autoAcceptCalls", value)
    }

    fun updateShakeDetection(value: Boolean) {
        updatePreference("enableShakeDetection", value)
    }

    fun updateNotificationSound(value: Boolean) {
        updatePreference("notificationSound", value)
    }

    private fun updatePreference(key: String, value: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).update(key, value)
            .addOnSuccessListener {
                val current = _preferencesState.value ?: UserPreferences()
                val updated = when (key) {
                    "autoAcceptCalls" -> current.copy(autoAcceptCalls = value)
                    "enableShakeDetection" -> current.copy(enableShakeDetection = value)
                    "notificationSound" -> current.copy(notificationSound = value)
                    else -> current
                }
                _preferencesState.value = updated
            }
    }

    fun syncToLocal(context: Context) {
        val prefs = _preferencesState.value ?: return
        EncryptedPreferencesHelper.saveBoolean(context, "auto_accept_calls", prefs.autoAcceptCalls)
        EncryptedPreferencesHelper.saveBoolean(context, "enable_shake", prefs.enableShakeDetection)
        EncryptedPreferencesHelper.saveBoolean(context, "notification_sound", prefs.notificationSound)
    }
}
