package com.example.monitordecuidados.models

sealed class NotificationState {
    object Loading : NotificationState()
    object Success : NotificationState()
    data class Error(val message: String) : NotificationState()
    object Empty : NotificationState()
}

data class NotificationItem(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val read: Boolean = false
)
