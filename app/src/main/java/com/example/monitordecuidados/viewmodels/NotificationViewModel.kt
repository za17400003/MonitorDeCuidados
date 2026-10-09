package com.example.monitordecuidados.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.example.monitordecuidados.cloud.FirebaseService
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.data.local.Event
import com.example.monitordecuidados.models.NotificationItem
import com.example.monitordecuidados.models.NotificationState
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val _notificationState = MutableLiveData<NotificationState>(NotificationState.Empty)
    val notificationState: LiveData<NotificationState> = _notificationState

    // T69: DashboardFragment LOCAL-FIRST — leer alertas de Room DB
    private val eventDao = AppDatabase.getDatabase(application).eventDao()

    // T84: 1 card por terminal, no 1 card por evento
    val notificationList: LiveData<List<Event>> = eventDao.getGroupedAlertsByTerminal(20)

    val unreadCount: LiveData<Int> = notificationList.map { list ->
        // Note: Event doesn't have a 'read' field yet, we might need to add it or use a separate way to track it.
        // For now, returning 0 to avoid build errors if the UI expects it.
        0
    }

    private val db = FirebaseService.db
    private val auth = FirebaseService.auth

    // T69: Keeping Firestore sync as backup, but Dashboard now reads from Room
    fun getNotifications() {
        val userId = auth.currentUser?.uid ?: return
        _notificationState.value = NotificationState.Loading
        
        db.collection("notifications_history")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                _notificationState.value = if (snapshot.isEmpty) NotificationState.Empty else NotificationState.Success
            }
            .addOnFailureListener { e ->
                _notificationState.value = NotificationState.Error(e.message ?: "Failed to load notifications")
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
                    _notificationState.value = if (snapshot.isEmpty) NotificationState.Empty else NotificationState.Success
                }
            }
    }
}