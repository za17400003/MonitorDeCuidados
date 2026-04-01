package com.example.monitordecuidados.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
    private var alertNotificationId = 100  // Incrementa para cada alerta nueva

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

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Cuidados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones acumuladas del servicio de Monitor"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
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
     */
    fun notifyAlert(context: Context, title: String, message: String, type: String, terminalName: String = "") {
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

        // Only show visible heads-up if we are in Monitor role
        if (CampanaService.userRole != CampanaService.ROLE_MONITOR) return

        // Show visible heads-up notification for the user
        val intent = Intent(context, MonitorMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, alertNotificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(alertNotificationId++, notification)
        if (alertNotificationId > 200) alertNotificationId = 100  // Reciclar IDs
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