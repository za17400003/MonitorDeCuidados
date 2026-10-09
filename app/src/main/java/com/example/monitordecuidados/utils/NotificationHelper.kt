package com.example.monitordecuidados.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.monitordecuidados.CampanaService
import com.example.monitordecuidados.MonitorMainActivity
import com.example.monitordecuidados.R

/**
 * Helper class for managing notifications in the Monitor app.
 * Refactored in M9: Unify alerts into Service notification.
 */
object NotificationHelper {
    const val SERVICE_CHANNEL_ID = "service_channel"
    const val ALERT_CHANNEL_ID = "alerts"
    const val BATTERY_CHANNEL_ID = "channel_battery"
    const val ALARMS_CHANNEL_ID = "channel_alarms"
    const val CALLS_CHANNEL_ID = "channel_calls"
    const val LOCKSCREEN_CHANNEL_ID = "lockscreen_bell"
    
    private val alertHistory = mutableListOf<AlertEvent>()

    data class AlertEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val title: String,
        val message: String,
        val type: String,
        val terminalName: String = ""
    )

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Servicio en Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

            // T83b: Eliminar canal existente para forzar recreación con allowBubbles=true.
            // createNotificationChannel() NO actualiza allowBubbles en canales ya existentes.
            manager.deleteNotificationChannel(ALERT_CHANNEL_ID)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Cuidados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones acumuladas del servicio de Monitor"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            manager.createNotificationChannel(alertChannel)

            val batteryChannel = NotificationChannel(
                BATTERY_CHANNEL_ID, "Batería", NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(batteryChannel)
            
            val alarmsChannel = NotificationChannel(
                ALARMS_CHANNEL_ID, "Alarmas", NotificationManager.IMPORTANCE_MAX
            )
            manager.createNotificationChannel(alarmsChannel)
            
            val callsChannel = NotificationChannel(
                CALLS_CHANNEL_ID, "Llamadas", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(callsChannel)

            val lockscreenChannel = NotificationChannel(
                LOCKSCREEN_CHANNEL_ID,
                "Campana en Pantalla de Bloqueo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Muestra la campana cuando se bloquea la pantalla"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            manager.createNotificationChannel(lockscreenChannel)
        }
    }

    /**
     * M9 Refactor: Adds to history and triggers service notification update.
     * T19: Added visible heads-up notification.
     * AUDIT FIX: Only show heads-up for Monitor role.
     * T80: Delegate to BubbleManager for per-terminal bubbles.
     */
    fun notifyAlert(context: Context, title: String, message: String, type: String, terminalName: String = "") {
        Log.d("NotificationHelper", "T68-TRACE: notifyAlert called, CampanaService.userRole=${CampanaService.userRole}, type=$type")

        if (message.isNotBlank()) {
            alertHistory.add(AlertEvent(
                title = title,
                message = message,
                type = type,
                terminalName = terminalName
            ))
            if (alertHistory.size > 10) alertHistory.removeAt(0)
        }
        
        // Notify CampanaService to refresh its foreground notification
        CampanaService.updateServiceNotificationWithAlerts(context)

        // Only show visible notification if we are in Monitor role
        if (CampanaService.userRole != CampanaService.ROLE_MONITOR) return

        // T80: Delegate to BubbleManager — per-terminal bubbles on API 30+, fallback on older
        val resolvedTerminalName = resolveTerminalName(context, terminalName)
        val resolvedTerminalId = terminalName.ifBlank { "default_terminal" }
        TerminalBubbleManager.notifyTerminalAlert(
            context = context,
            terminalId = resolvedTerminalId,
            terminalName = resolvedTerminalName,
            terminalIp = terminalName.takeIf { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) },
            alertTitle = title,
            alertMessage = message
        )
    }

    private fun resolveTerminalName(context: Context, terminalNameOrIp: String): String {
        if (terminalNameOrIp.isBlank()) return "Terminal"
        if (terminalNameOrIp.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
            val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
            val savedName = prefs.getString("paired_terminal_name", null)
            return savedName ?: "Terminal ($terminalNameOrIp)"
        }
        return terminalNameOrIp
    }

    fun getAlertHistory() = alertHistory.toList()

    fun resetAlertCount() {
        alertHistory.clear()
    }

    fun clearNotification(context: Context) {
        resetAlertCount()
        // Update service notification to clear the list
        CampanaService.updateServiceNotificationWithAlerts(context)
    }
}
