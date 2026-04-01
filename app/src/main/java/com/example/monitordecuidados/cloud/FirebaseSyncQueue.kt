package com.example.monitordecuidados.cloud

import android.content.Context
import android.util.Log
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.data.local.Event
import com.example.monitordecuidados.CampanaService
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages offline operations and batch synchronization to Firestore.
 */
class FirebaseSyncQueue(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestore = FirebaseService.db
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        /**
         * Generic operation queuing for offline support.
         * Implementation for Phase 4B-GAP tasks.
         */
        fun queueOperation(collection: String, docId: String?, data: Map<String, Any?>) {
            Log.d("FirebaseSyncQueue", "Queuing operation for $collection: $data")
            // In a full implementation, this would save to a 'pending_operations' Room table.
            // For now, we rely on the Event-based sync for alerts.
        }
    }

    fun processSyncQueue() {
        if (!FirebaseService.isConnected(context)) return

        scope.launch {
            val unsyncedEvents = db.eventDao().getUnsyncedEvents()
            for (event in unsyncedEvents) {
                syncEventToCloud(event)
            }
        }
    }

    private fun syncEventToCloud(event: Event) {
        val userId = FirebaseService.getUserId() ?: "unknown"
        
        // Log to connection_logs
        val logEntry = mapOf(
            "monitorId" to (if (CampanaService.userRole == CampanaService.ROLE_MONITOR) userId else "remote"),
            "terminalId" to (if (CampanaService.userRole == CampanaService.ROLE_TERMINAL) userId else (event.sourceTerminalId.ifEmpty { event.sourceIp ?: "unknown" })),
            "connectionType" to FirebaseService.getConnectionType(context),
            "timestamp" to event.timestamp,
            "startTime" to FieldValue.serverTimestamp(),
            "eventType" to event.type,
            "message" to event.message,
            "status" to "successful"
        )

        firestore.collection("connection_logs")
            .add(logEntry)
            .addOnSuccessListener {
                scope.launch {
                    db.eventDao().markSynced(event.id)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSyncQueue", "Failed to sync connection log: ${e.message}")
            }
            
        // Task 3: Also log shake/voice to notifications_history
        if (event.type == "shake" || event.type == "voice") {
            val notificationData = mapOf(
                "userId" to userId,
                "senderId" to if (event.type == "voice") event.sourceIp else null,
                "type" to event.type,
                "title" to if (event.type == "shake") "🔔 Shake Detection Alert" else "🔔 Voice Alert",
                "body" to event.message,
                "timestamp" to FieldValue.serverTimestamp(),
                "read" to false,
                "actionTaken" to null
            )
            firestore.collection("notifications_history")
                .add(notificationData)
                .addOnFailureListener { e ->
                    Log.e("FirebaseSyncQueue", "Failed to sync notification history: ${e.message}")
                }
        }
    }
}
