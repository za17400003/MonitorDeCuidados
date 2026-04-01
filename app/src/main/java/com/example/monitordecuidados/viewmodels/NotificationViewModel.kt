package com.example.monitordecuidados.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.monitordecuidados.cloud.FirebaseService
import com.example.monitordecuidados.models.NotificationItem
import com.example.monitordecuidados.models.NotificationState
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot

class NotificationViewModel : ViewModel() {
    private val _notificationState = MutableLiveData<NotificationState>(NotificationState.Empty)
    val notificationState: LiveData<NotificationState> = _notificationState

    private val _notificationList = MutableLiveData<List<NotificationItem>>(emptyList())
    val notificationList: LiveData<List<NotificationItem>> = _notificationList

    val unreadCount: LiveData<Int> = _notificationList.map { list ->
        list.count { !it.read }
    }

    private val db = FirebaseService.db
    private val auth = FirebaseService.auth

    fun getNotifications() {
        val userId = auth.currentUser?.uid ?: return
        _notificationState.value = NotificationState.Loading
        
        db.collection("notifications_history")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    _notificationState.value = NotificationState.Empty
                    _notificationList.value = emptyList()
                } else {
                    val list = snapshot.documents.mapNotNull { doc ->
                        mapDocumentToNotification(doc as QueryDocumentSnapshot)
                    }
                    _notificationList.value = list
                    _notificationState.value = NotificationState.Success
                }
            }
            .addOnFailureListener { e ->
                _notificationState.value = NotificationState.Error(e.message ?: "Failed to load notifications")
            }
    }

    private fun mapDocumentToNotification(doc: QueryDocumentSnapshot): NotificationItem {
        val type = doc.getString("type") ?: "unknown"
        return NotificationItem(
            id = doc.id,
            type = type,
            title = getTitleForType(type),
            body = getBodyForType(doc),
            timestamp = doc.getTimestamp("timestamp"),
            read = doc.getBoolean("read") ?: false
        )
    }

    private fun getTitleForType(type: String?): String = when (type) {
        "battery_low" -> "Batería Baja"
        "battery_ok" -> "Batería Recuperada"
        "shake" -> "Alerta de Agitación"
        "voice" -> "Alerta de Voz"
        "bell" -> "Campana"
        "alarm" -> "Alarma"
        else -> "Alerta"
    }

    private fun getBodyForType(doc: QueryDocumentSnapshot): String {
        val type = doc.getString("type")
        val deviceName = doc.getString("deviceName") ?: "Dispositivo"
        val batteryLevel = doc.getLong("batteryLevel")?.toInt()
        return when (type) {
            "battery_low", "battery_ok" -> "$deviceName - ${batteryLevel}%"
            "voice" -> doc.getString("body") ?: "$deviceName - Voz"
            "shake" -> doc.getString("body") ?: "$deviceName - Agitación"
            "alarm" -> doc.getString("body") ?: "$deviceName - Alarma"
            else -> doc.getString("body") ?: deviceName
        }
    }

    fun markAsRead(notificationId: String) {
        db.collection("notifications_history").document(notificationId)
            .update("read", true)
            .addOnSuccessListener {
                val currentList = _notificationList.value ?: return@addOnSuccessListener
                val updatedList = currentList.map {
                    if (it.id == notificationId) it.copy(read = true) else it
                }
                _notificationList.value = updatedList
            }
    }

    fun observeRealtimeNotifications() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("notifications_history")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _notificationState.value = NotificationState.Error(e.message ?: "Real-time error")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        _notificationState.value = NotificationState.Empty
                        _notificationList.value = emptyList()
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            mapDocumentToNotification(doc as QueryDocumentSnapshot)
                        }
                        _notificationList.value = list
                        _notificationState.value = NotificationState.Success
                    }
                }
            }
    }
}