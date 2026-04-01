package com.example.monitordecuidados.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.monitordecuidados.CampanaService
import com.example.monitordecuidados.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        val role = prefs.getString("user_role", null) ?: return
        
        // 1. Iniciar servicio si es Terminal
        if (role == "terminal") {
            val campanaIntent = Intent(context, CampanaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(campanaIntent)
            } else {
                context.startService(campanaIntent)
            }
        }
        
        // 2. Reprogramar todas las alarmas recurrentes
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val alarms = db.alarmDao().getAllAlarmsSync()
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                
                for (alarm in alarms) {
                    if (alarm.repeatMode == "once") continue
                    if (!alarm.enabled) continue
                    
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                    }

                    // Si la hora ya pasó hoy, buscar la siguiente ocurrencia
                    if (calendar.before(Calendar.getInstance())) {
                        calendar.add(Calendar.DATE, 1)
                    }
                    
                    if (alarm.repeatMode == "weekdays") {
                        while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                               calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                            calendar.add(Calendar.DATE, 1)
                        }
                    }
                    
                    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("alarm_id", alarm.id)
                        putExtra("message", alarm.message)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, alarm.id, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
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
                        Log.d("BootReceiver", "Rescheduled alarm ${alarm.id} for ${calendar.time}")
                    } catch (e: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error rescheduling alarms: ${e.message}")
            }
        }
    }
}