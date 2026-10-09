package com.example.monitordecuidados.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.monitordecuidados.cloud.FirebaseService
import com.example.monitordecuidados.models.UserData
import com.example.monitordecuidados.models.UserPreferences
import com.example.monitordecuidados.models.UserState
import com.google.firebase.auth.GoogleAuthProvider

class UserViewModel : ViewModel() {

    private val _userState = MutableLiveData<UserState>(UserState.Initial)
    val userState: LiveData<UserState> = _userState

    private val _preferences = MutableLiveData<UserPreferences>()
    val preferences: LiveData<UserPreferences> = _preferences

    private val auth by lazy { FirebaseService.auth }
    private val db by lazy { FirebaseService.db }

    fun login(email: String, password: String) {
        _userState.value = UserState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val userData = UserData(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            fullName = firebaseUser.displayName,
                            role = null
                        )
                        _userState.value = UserState.Success(userData)
                        loadPreferences()
                    } else {
                        _userState.value = UserState.Error("User is null after login")
                    }
                } else {
                    _userState.value = UserState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun loginWithGoogle(idToken: String) {
        _userState.value = UserState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val userData = UserData(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            fullName = firebaseUser.displayName,
                            role = null
                        )
                        _userState.value = UserState.Success(userData)
                        loadPreferences()
                    } else {
                        _userState.value = UserState.Error("User is null after Google login")
                    }
                } else {
                    _userState.value = UserState.Error(task.exception?.message ?: "Google login failed")
                }
            }
    }

    fun logout() {
        auth.signOut()
        _userState.value = UserState.Initial
    }

    fun loadPreferences() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val prefs = UserPreferences(
                        autoAcceptCalls = document.getBoolean("autoAcceptCalls") ?: false,
                        enableShakeDetection = document.getBoolean("enableShakeDetection") ?: true,
                        notificationSound = document.getBoolean("notificationSound") ?: true
                    )
                    _preferences.value = prefs
                } else {
                    val defaultPrefs = UserPreferences()
                    _preferences.value = defaultPrefs
                    db.collection("users").document(userId).set(defaultPrefs)
                }
            }
            .addOnFailureListener {
                _preferences.value = UserPreferences()
            }
    }

    fun updatePreference(key: String, value: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).update(key, value)
            .addOnSuccessListener {
                val currentPrefs = _preferences.value ?: UserPreferences()
                val updatedPrefs = when (key) {
                    "autoAcceptCalls" -> currentPrefs.copy(autoAcceptCalls = value)
                    "enableShakeDetection" -> currentPrefs.copy(enableShakeDetection = value)
                    "notificationSound" -> currentPrefs.copy(notificationSound = value)
                    else -> currentPrefs
                }
                _preferences.value = updatedPrefs
            }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
