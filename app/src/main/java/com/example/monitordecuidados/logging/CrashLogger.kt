package com.example.monitordecuidados.logging

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.monitordecuidados.BuildConfig
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Global Crash Logger.
 * Captures uncaught exceptions and saves them to local storage.
 *
 * SPEC SEC-02
 */
object CrashLogger {
    private var logFile: File? = null
    
    /**
     * Initializes the crash logger.
     * @param context Application context
     */
    fun initialize(context: Context) {
        try {
            val crashDir = File(context.getExternalFilesDir(null), "crashes")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            logFile = File(crashDir, "crash_$timestamp.log")
            Log.i("CrashLogger", "Logging to ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("CrashLogger", "Failed to initialize", e)
        }
    }
    
    /**
     * Logs an uncaught exception with device and version info.
     * @param exception The exception that caused the crash
     */
    fun logUncaughtException(exception: Throwable) {
        val file = logFile ?: return
        try {
            FileWriter(file, true).use { writer ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val stackTrace = Log.getStackTraceString(exception)
                
                writer.write("""
                    [$timestamp] UNCAUGHT EXCEPTION
                    Exception: ${exception.javaClass.simpleName}
                    Message: ${exception.message}
                    
                    Stack Trace:
                    $stackTrace
                    
                    Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                    Device: ${Build.MANUFACTURER} ${Build.MODEL}
                    App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                    
                    ---
                    
                """.trimIndent())
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e("CrashLogger", "Failed to log crash", e)
        }
    }
    
    /**
     * Returns the content of the current log file.
     * @return Log content or error message
     */
    fun getCurrentLogs(): String {
        return try {
            logFile?.readText() ?: "Logger not initialized"
        } catch (e: Exception) {
            "Unable to read logs: ${e.message}"
        }
    }
}
