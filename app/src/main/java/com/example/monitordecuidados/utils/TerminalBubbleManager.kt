package com.example.monitordecuidados.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.monitordecuidados.R
import com.example.monitordecuidados.BubbleRadialActivity

/**
 * Manages per-terminal notification bubbles (API 30+).
 * T83: Suppressed fallback on API < 30 (handled by service notification).
 * T85: Updated targets to BubbleRadialActivity.
 * T83b: Fix notification duplication + areBubblesAllowed guard.
 * Architecture: 1 Monitor : N Terminals → N Bubbles
 */
object TerminalBubbleManager {

    private const val TAG = "TerminalBubbleManager"
    private const val BUBBLE_NOTIFICATION_ID_BASE = 200
    private const val SHORTCUT_CATEGORY = "com.example.monitordecuidados.category.TERMINAL_BUBBLE"

    private val terminalAlertCounts = mutableMapOf<String, Int>()

    fun notifyTerminalAlert(
        context: Context,
        terminalId: String,
        terminalName: String,
        terminalIp: String?,
        alertTitle: String,
        alertMessage: String
    ) {
        val count = (terminalAlertCounts[terminalId] ?: 0) + 1
        terminalAlertCounts[terminalId] = count

        val notificationId = BUBBLE_NOTIFICATION_ID_BASE + terminalId.hashCode().and(0xFF)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // T83: Solo burbujas en API 30+. En < 30 no duplicar notificación (ya está la del servicio).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showBubbleNotification(context, nm, notificationId, terminalId, terminalName, terminalIp, alertTitle, alertMessage, count)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showBubbleNotification(
        context: Context,
        nm: NotificationManager,
        notificationId: Int,
        terminalId: String,
        terminalName: String,
        terminalIp: String?,
        alertTitle: String,
        alertMessage: String,
        alertCount: Int
    ) {
        // T83b: Si burbujas no están habilitadas en el sistema, NO crear notificación.
        // La notificación del servicio (ID=1) ya muestra las alertas acumuladas.
        if (!nm.areBubblesAllowed()) {
            Log.d(TAG, "Bubbles not allowed by system — skipping bubble notification for '$terminalName'")
            return
        }

        val shortcutId = "terminal_$terminalId"
        val person = Person.Builder()
            .setName(terminalName)
            .setKey(terminalId)
            .setImportant(true)
            .build()

        // T85: Apuntar a BubbleRadialActivity
        val shortcutIntent = Intent(context, BubbleRadialActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_ip", terminalIp)
            putExtra("alert_title", alertTitle)
            putExtra("alert_count", alertCount)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(terminalName)
            .setLongLabel("Terminal: $terminalName")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_notification_bell))
            .setIntent(shortcutIntent)
            .setLongLived(true)
            .setPerson(person)
            .setCategories(setOf(SHORTCUT_CATEGORY))
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        // T85: Apuntar a BubbleRadialActivity
        val bubbleIntent = Intent(context, BubbleRadialActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_ip", terminalIp)
            putExtra("alert_title", alertTitle)
            putExtra("alert_count", alertCount)
            putExtra("from_bubble", true)
        }
        val bubblePendingIntent = PendingIntent.getActivity(
            context, notificationId, bubbleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
            bubblePendingIntent,
            IconCompat.createWithResource(context, R.drawable.ic_notification_bell)
        )
            .setDesiredHeight(350)
            .setAutoExpandBubble(alertCount == 1)
            .setSuppressNotification(true) // T83b: SIEMPRE suprimir del shade. Solo mostrar como burbuja flotante.
            .build()

        val displayTitle = if (alertCount > 1) "$alertTitle (+${alertCount - 1})" else alertTitle
        val notification = NotificationCompat.Builder(context, NotificationHelper.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(displayTitle)
            .setContentText(alertMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(shortcutId)
            .setBubbleMetadata(bubbleMetadata)
            .addPerson(person)
            .setNumber(alertCount)
            .setAutoCancel(false)
            .setOnlyAlertOnce(alertCount > 1)
            .build()

        nm.notify(notificationId, notification)
        Log.d(TAG, "Bubble for '$terminalName' (id=$notificationId, alerts=$alertCount, suppressed=true)")
    }

    fun clearTerminalAlerts(terminalId: String) {
        terminalAlertCounts.remove(terminalId)
    }

    fun clearAllAlerts() {
        terminalAlertCounts.clear()
    }

    fun getAlertCount(terminalId: String): Int {
        return terminalAlertCounts[terminalId] ?: 0
    }
}
