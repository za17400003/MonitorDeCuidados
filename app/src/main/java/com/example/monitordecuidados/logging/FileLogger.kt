package com.example.monitordecuidados.logging

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Secure Logging to File.
 * All important operations logged to persistent local file.
 *
 * SPEC SEC-03
 */
object FileLogger {
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024  // 5MB
    private const val MAX_LOG_FILES = 7
    private const val TAG = "FileLogger"
    
    /**
     * Logs a critical error.
     */
    fun logCritical(tag: String, message: String, exception: Throwable? = null) {
        writeLog("ERROR", tag, message, exception)
    }
    
    /**
     * Logs a warning.
     */
    fun logWarning(tag: String, message: String) {
        writeLog("WARN", tag, message)
    }
    
    /**
     * Logs informational message.
     */
    fun logInfo(tag: String, message: String) {
        writeLog("INFO", tag, message)
    }
    
    /**
     * Logs debug message.
     */
    fun logDebug(tag: String, message: String) {
        writeLog("DEBUG", tag, message)
    }
    
    private fun writeLog(level: String, tag: String, message: String, exception: Throwable? = null) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $level/$tag: $message"
        
        // Also log to Logcat
        when (level) {
            "ERROR" -> Log.e(tag, message, exception)
            "WARN" -> Log.w(tag, message, exception)
            "INFO" -> Log.i(tag, message)
            else -> Log.d(tag, message)
        }
        
        // Write to file
        try {
            val logFile = getCurrentLogFile()
            FileWriter(logFile, true).use { writer ->
                writer.write(logMessage)
                if (exception != null) {
                    writer.write("\n${Log.getStackTraceString(exception)}")
                }
                writer.write("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }
    
    private fun getCurrentLogFile(): File {
        val logDir = File(Environment.getExternalStorageDirectory(), "MonitorDeCuidados/logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayLog = File(logDir, "app_$dateStr.log")
        
        // Rotate if current log > 5MB
        if (todayLog.exists() && todayLog.length() > MAX_LOG_SIZE) {
            rotateOldLogs(logDir)
        }
        
        return todayLog
    }
    
    private fun rotateOldLogs(logDir: File) {
        val files = logDir.listFiles { file -> file.extension == "log" }?.sortedByDescending { it.lastModified() }
        
        // Delete logs older than 7 days
        files?.drop(MAX_LOG_FILES)?.forEach { it.delete() }
    }
}
