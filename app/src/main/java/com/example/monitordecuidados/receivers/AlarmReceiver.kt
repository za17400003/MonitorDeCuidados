package com.example.monitordecuidados.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.util.Log
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val message = intent.getStringExtra("message") ?: "Alarma de Monitor de Cuidados"
        val terminalName = intent.getStringExtra("terminal_name") ?: ""
        
        Log.d("AlarmReceiver", "Alarm received! ID: $alarmId, Message: $message")

        // 1. Mostrar notificación
        NotificationHelper.notifyAlert(context, "Alarma", "⏰ $message", "alarm", terminalName)
        
        // 2. Sonido y Vibración
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone.play()
            
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(2000)
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error playing alarm sound/vibration: ${e.message}")
        }

        // 3. Reprogramar si es recurrente
        if (alarmId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch
                    
                    if (alarm.repeatMode == "once") return@launch
                    
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                    }
                    
                    // Si ya pasó hoy (que es lo normal al recibir la alarma), sumamos un día para empezar a buscar la siguiente
                    calendar.add(Calendar.DATE, 1)

                    if (alarm.repeatMode == "weekdays") {
                        while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                               calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                            calendar.add(Calendar.DATE, 1)
                        }
                    }
                    
                    val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("alarm_id", alarm.id)
                        putExtra("message", alarm.message)
                        putExtra("terminal_name", terminalName)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, alarm.id, nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                            } else {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                        }
                    } catch (e: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }
                    Log.d("AlarmReceiver", "Rescheduled alarm $alarmId for ${calendar.time}")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error rescheduling alarm: ${e.message}")
                }
            }
        }
    }
}