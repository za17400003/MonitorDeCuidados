package com.example.monitordecuidados

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.monitordecuidados.logging.CrashLogger
import com.example.monitordecuidados.sync.SyncManager
import com.example.monitordecuidados.utils.StringsLocalizationManager
import com.example.monitordecuidados.cloud.FirebaseSyncQueue
import com.example.monitordecuidados.utils.NotificationHelper
import com.google.firebase.FirebaseApp
import net.sqlcipher.database.SQLiteDatabase

/**
 * Main Application class for Monitor de Cuidados.
 */
class CareMonitorApp : Application() {
    
    lateinit var syncManager: SyncManager
        private set
        
    lateinit var firebaseSyncQueue: FirebaseSyncQueue
        private set

    override fun onCreate() {
        super.onCreate()
        
        CrashLogger.initialize(this)
        setupGlobalExceptionHandler()
        
        try {
            FirebaseApp.initializeApp(this)
            Log.d("CareMonitorApp", "Firebase initialized successfully")
            StringsLocalizationManager.init(this)
        } catch (e: Exception) {
            Log.e("CareMonitorApp", "Firebase initialization failed: ${e.message}")
        }
        
        SQLiteDatabase.loadLibs(this)
        syncManager = SyncManager(this)
        firebaseSyncQueue = FirebaseSyncQueue(this)
        
        // BUG T4: Persistent Night Mode
        val prefs = getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        val nightMode = prefs.getBoolean("night_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (nightMode) AppCompatDelegate.MODE_NIGHT_YES
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        createNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        syncManager.startAutoSync()
                        firebaseSyncQueue.processSyncQueue()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        syncManager.stopAutoSync()
                    }
                    else -> {}
                }
            }
        })
    }

    private fun createNotificationChannels() {
        NotificationHelper.createNotificationChannels(this)
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            CrashLogger.logUncaughtException(exception)
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}