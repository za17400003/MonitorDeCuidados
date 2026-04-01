package com.example.monitordecuidados.sync

import android.content.Context
import android.util.Log
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.data.local.Event
import com.example.monitordecuidados.data.local.Alarm
import com.example.monitordecuidados.data.local.CustomPhrase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.math.pow

class SyncManager(private val context: Context) {
    private val localDb by lazy { AppDatabase.getDatabase(context) }
    private val firebaseDb by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val syncScope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null

    private var retryCount = 0
    private val maxRetries = 3
    private val baseRetryDelayMs = 2000L // 2 seconds
    private val maxRetryDelayMs = 300000L // 5 minutes (as per spec)

    fun startAutoSync(intervalMs: Long = 30000) {
        stopAutoSync()
        syncJob = syncScope.launch {
            while (isActive) {
                try {
                    val user = auth.currentUser
                    if (user != null) {
                        val success = performSync(user.uid)
                        if (success) {
                            retryCount = 0
                            delay(intervalMs)
                        } else {
                            handleSyncFailure(intervalMs)
                        }
                    } else {
                        delay(intervalMs)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("SyncManager", "Sync loop error: ${e.message}")
                    handleSyncFailure(intervalMs)
                }
            }
        }
    }

    private suspend fun performSync(userId: String): Boolean {
        return try {
            coroutineScope {
                val eventsJob = async { syncEventsToCloud(userId) }
                val alarmsJob = async { syncAlarms(userId) }
                val phrasesJob = async { syncCustomPhrases(userId) }
                val configJob = async { syncConfigFromCloud(userId) }
                
                eventsJob.await() && alarmsJob.await() && phrasesJob.await() && configJob.await()
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync performance failed: ${e.message}")
            false
        }
    }

    private suspend fun handleSyncFailure(baseIntervalMs: Long) {
        if (retryCount < maxRetries) {
            retryCount++
            val backoffDelay = (baseRetryDelayMs * 2.0.pow(retryCount.toDouble())).toLong()
            val finalDelay = minOf(backoffDelay, maxRetryDelayMs)
            Log.i("SyncManager", "Sync failed, retrying in ${finalDelay / 1000}s (Retry $retryCount/$maxRetries)")
            delay(finalDelay)
        } else {
            Log.w("SyncManager", "Max retries reached, waiting for next cycle.")
            retryCount = 0 // Reset and wait for the normal cycle
            delay(baseIntervalMs)
        }
    }

    private suspend fun syncEventsToCloud(userId: String): Boolean {
        var allSuccess = true
        val unsyncedEvents = localDb.eventDao().getUnsyncedEvents()
        
        for (event in unsyncedEvents) {
            try {
                val eventMap = hashMapOf(
                    "timestamp" to event.timestamp,
                    "type" to event.type,
                    "message" to event.message,
                    "sourceIp" to event.sourceIp,
                    "sourceTerminalName" to event.sourceTerminalName, // P3-2: Included name
                    "userId" to userId
                )
                
                firebaseDb.collection("users")
                    .document(userId)
                    .collection("events")
                    .document(event.id.toString())
                    .set(eventMap)
                    .await()
                
                localDb.eventDao().markSynced(event.id)
            } catch (e: Exception) {
                Log.w("SyncManager", "Failed to sync event ${event.id}: ${e.message}")
                allSuccess = false
            }
        }
        return allSuccess
    }

    private suspend fun syncAlarms(userId: String): Boolean {
        return try {
            val localAlarms = localDb.alarmDao().getAllAlarmsSync()
            for (alarm in localAlarms) {
                val alarmMap = hashMapOf(
                    "hour" to alarm.hour,
                    "minute" to alarm.minute,
                    "message" to alarm.message,
                    "repeatMode" to alarm.repeatMode,
                    "enabled" to alarm.enabled,
                    "soundUri" to alarm.soundUri,
                    "terminalId" to alarm.terminalId,
                    "createdBy" to alarm.createdBy
                )
                firebaseDb.collection("users")
                    .document(userId)
                    .collection("alarms")
                    .document(alarm.id.toString())
                    .set(alarmMap)
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.w("SyncManager", "Failed to sync alarms: ${e.message}")
            false
        }
    }

    private suspend fun syncCustomPhrases(userId: String): Boolean {
        return try {
            val phrases = localDb.customPhraseDao().getAllPhrasesSync()
            for (phrase in phrases) {
                val phraseMap = hashMapOf(
                    "name" to phrase.name,
                    "confidence" to phrase.confidence,
                    "createdAt" to phrase.createdAt,
                    "encryptedContent" to phrase.encryptedContent,
                    "autoAdded" to phrase.autoAdded,
                    "addedBy_monitorId" to phrase.addedBy_monitorId
                )
                firebaseDb.collection("users")
                    .document(userId)
                    .collection("custom_phrases")
                    .document(phrase.name)
                    .set(phraseMap)
                    .await()
            }
            true
        } catch (e: Exception) {
            Log.w("SyncManager", "Failed to sync custom phrases: ${e.message}")
            false
        }
    }

    private suspend fun syncConfigFromCloud(userId: String): Boolean {
        return try {
            val config = firebaseDb.collection("users")
                .document(userId)
                .collection("config")
                .document("settings")
                .get()
                .await()
            
            if (config.exists()) {
                val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                config.data?.forEach { (key, value) ->
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Long -> editor.putInt(key, value.toInt())
                        is Double -> editor.putFloat(key, value.toFloat())
                    }
                }
                editor.apply()
            }
            true
        } catch (e: Exception) {
            Log.w("SyncManager", "Failed to sync config: ${e.message}")
            false
        }
    }

    fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
    }
}
