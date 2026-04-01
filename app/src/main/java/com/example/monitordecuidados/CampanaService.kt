package com.example.monitordecuidados

import android.app.*
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.monitordecuidados.communication.CampanaHttpServer
import com.example.monitordecuidados.communication.CallManager
import com.example.monitordecuidados.communication.VoiceCommandManager
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.data.local.Event
import com.example.monitordecuidados.logging.FileLogger
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.utils.NetworkUtils
import com.example.monitordecuidados.utils.NotificationHelper
import com.example.monitordecuidados.utils.StringsLocalizationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import kotlin.math.sqrt

class CampanaService : Service(), SensorEventListener {

    companion object {
        const val ROLE_MONITOR = "monitor"
        const val ROLE_TERMINAL = "terminal"
        const val ACTION_TRIGGER_BELL = "com.example.monitordecuidados.TRIGGER_BELL"
        const val SERVICE_TYPE = "_caremonitor._tcp"
        private const val LOCKSCREEN_NOTIFICATION_ID = 2
        var isRunning = false
        var userRole: String = ""

        fun startService(context: Context) {
            val intent = Intent(context, CampanaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CampanaService::class.java)
            context.stopService(intent)
        }

        fun updateServiceNotificationWithAlerts(context: Context) {
            val intent = Intent(context, CampanaService::class.java).apply {
                action = "UPDATE_ALERTS"
            }
            context.startService(intent)
        }

        fun refreshService(context: Context) {
            val intent = Intent(context, CampanaService::class.java).apply {
                action = "REFRESH_SERVICES"
            }
            context.startService(intent)
        }
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private var isAudioCallActive = false
    private var server: CampanaHttpServer? = null
    private var screenOffReceiver: BroadcastReceiver? = null
    private var voiceCommandManager: VoiceCommandManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "CampanaService"

    // T46: CallManager instance
    private var callManager: CallManager? = null

    // T53: Battery monitoring
    private var lastBatteryAlertState: String = "ok" // "ok" | "low"
    private val batteryCheckHandler = Handler(Looper.getMainLooper())
    private val batteryCheckRunnable = object : Runnable {
        override fun run() {
            checkBatteryLevel()
            batteryCheckHandler.postDelayed(this, 60000)
        }
    }

    private val serverListener = object : CampanaHttpServer.OnServerEventListener {
        override fun onBellTriggered(sourceIp: String) {
            NotificationHelper.notifyAlert(this@CampanaService, "🔔 Campana", "Se ha pedido ayuda desde Terminal", "bell", sourceIp)
        }
        override fun onVoiceTriggered(text: String, sourceIp: String) {
            NotificationHelper.notifyAlert(this@CampanaService, "🎙️ Voz", text, "voice", sourceIp)
        }
        
        // T58: Walkie-talkie side for Terminal
        override fun onCallRequested(sourceIp: String) {
            FileLogger.logInfo(TAG, "onCallRequested from $sourceIp — starting walkie-talkie")
            callManager?.stopCall()
            callManager = CallManager(sourceIp, 5060)
            callManager?.startCall()
            isAudioCallActive = true
            updateNotification()
        }
        
        // T59: Silent monitor request with auto-accept
        override fun onMonitorRequested(sourceIp: String) {
            val intent = Intent(this@CampanaService, VideoActivity::class.java).apply {
                putExtra("mode", "monitor")
                putExtra("remote_ip", sourceIp)
                putExtra("auto_accept", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
        
        override fun onCommandReceived(command: String, params: JSONObject) {
            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            when (command) {
                "SET_BELL_MODE_ON" -> prefs.edit().putBoolean("lockscreen_bell_enabled", true).apply()
                "SET_BELL_MODE_OFF" -> prefs.edit().putBoolean("lockscreen_bell_enabled", false).apply()
                "SET_SHAKE_ON" -> prefs.edit().putBoolean("shake_detection_enabled", true).apply()
                "SET_SHAKE_OFF" -> prefs.edit().putBoolean("shake_detection_enabled", false).apply()
                "SET_VOICE_ON" -> prefs.edit().putBoolean("voice_detection_enabled", true).apply()
                "SET_VOICE_OFF" -> prefs.edit().putBoolean("voice_detection_enabled", false).apply()
                "SET_ALARMS_ON" -> prefs.edit().putBoolean("reminders_enabled", true).apply()
                "SET_ALARMS_OFF" -> prefs.edit().putBoolean("reminders_enabled", false).apply()
                // T58: Stop audio call command
                "STOP_AUDIO_CALL" -> {
                    callManager?.stopCall()
                    callManager = null
                    isAudioCallActive = false
                    updateNotification()
                    return
                }
                else -> return // Comandos desconocidos se ignoran
            }
            // Aplicar cambio inmediatamente en main thread
            Handler(Looper.getMainLooper()).post { refreshServices() }
        }

        // T57: Pairing diagnostics
        override fun onPairingConfirmed(sourceIp: String, monitorName: String) {
            FileLogger.logInfo(TAG, "onPairingConfirmed: sourceIp=$sourceIp, monitorName=$monitorName")
            getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE).edit()
                .putString("paired_monitor_ip", sourceIp)
                .putString("paired_monitor_name", monitorName)
                .apply()
            // Verificar que se guardó
            val saved = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
                .getString("paired_monitor_ip", null)
            FileLogger.logInfo(TAG, "paired_monitor_ip saved as: $saved")
        }
        override fun onShakeTriggered(sourceIp: String) {
            NotificationHelper.notifyAlert(this@CampanaService, "⚠️ Caída detectada", "Movimiento brusco en Terminal", "shake", sourceIp)
        }

        override fun onStatusRequested(): JSONObject {
            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            return JSONObject().apply {
                put("batteryLevel", batteryLevel)
                put("deviceName", prefs.getString("terminal_person_name", android.os.Build.MODEL))
                put("bellEnabled", prefs.getBoolean("lockscreen_bell_enabled", true))
                put("shakeEnabled", prefs.getBoolean("shake_detection_enabled", true))
                put("voiceEnabled", prefs.getBoolean("voice_detection_enabled", true))
                put("alarmsEnabled", prefs.getBoolean("reminders_enabled", true))
                put("status", "connected")
                put("timestamp", System.currentTimeMillis())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        userRole = prefs.getString("user_role", ROLE_TERMINAL) ?: ROLE_TERMINAL

        if (userRole == ROLE_TERMINAL) {
            setupShakeDetection()
            setupScreenOffReceiver()
            startHttpServer()
            setupVoiceDetection()
            // T53: Start battery monitoring
            batteryCheckHandler.post(batteryCheckRunnable)
        } else if (userRole == ROLE_MONITOR) {
            startHttpServer()
        }

        // T55: Init NetworkUtils
        NetworkUtils.initConnectionType(this)
        NetworkUtils.registerNetworkCallback(this) { hasInternet ->
            if (!hasInternet) {
                FileLogger.logWarning(TAG, "Network lost")
            } else {
                FileLogger.logInfo(TAG, "Network available")
            }
        }

        startForeground(1, createServiceNotification())
    }

    private fun startHttpServer() {
        try {
            server = CampanaHttpServer(8080, serverListener)
            server?.start()
            Log.d(TAG, "HTTP Server started on port 8080")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTP Server", e)
        }
    }

    private fun setupShakeDetection() {
        val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("shake_detection_enabled", true)) return

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    private fun setupScreenOffReceiver() {
        val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("lockscreen_bell_enabled", true)) return

        if (screenOffReceiver == null) {
            screenOffReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        Intent.ACTION_SCREEN_OFF -> showLockscreenBell()
                        Intent.ACTION_SCREEN_ON -> {
                            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            nm.cancel(LOCKSCREEN_NOTIFICATION_ID)
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenOffReceiver, filter)
        }
    }

    private fun showLockscreenBell() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "monitordecuidados:lockscreen_bell"
        )
        wakeLock.acquire(5000L)

        val bellIntent = Intent(this, BellActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, bellIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.LOCKSCREEN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle("Campana de Cuidados")
            .setContentText("Toca para abrir")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(LOCKSCREEN_NOTIFICATION_ID, notification)
    }

    private fun setupVoiceDetection() {
        val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("voice_detection_enabled", true)) return

        if (android.content.pm.PackageManager.PERMISSION_GRANTED != 
            checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)) {
            Log.w(TAG, "setupVoiceDetection: RECORD_AUDIO not granted, skipping")
            prefs.edit().putBoolean("voice_detection_enabled", false).apply()
            return
        }

        voiceCommandManager = VoiceCommandManager(this, object : VoiceCommandManager.OnVoiceCommandListener {
            override fun onKeywordDetected(keyword: String) {
                sendVoiceAlert(keyword)
            }
            override fun onSpeechError(error: String) {
                Log.w(TAG, "Voice detection error: $error")
            }
        })
        voiceCommandManager?.startListening()
    }

    private fun refreshServices() {
        sensorManager?.unregisterListener(this)
        screenOffReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
            screenOffReceiver = null
        }
        voiceCommandManager?.stopListening()
        voiceCommandManager = null
        if (userRole == ROLE_TERMINAL) {
            setupShakeDetection()
            setupScreenOffReceiver()
            setupVoiceDetection()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
            if (acceleration > 12) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 2000) {
                    lastShakeTime = currentTime
                    sendShakeAlert()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sendShakeAlert() {
        if (userRole == ROLE_TERMINAL) {
            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            val sourceTerminalName = prefs.getString("user_name", "Terminal") ?: "Terminal"

            serviceScope.launch {
                try {
                    val db = AppDatabase.getDatabase(this@CampanaService)
                    val shakeMsg = StringsLocalizationManager.getString(this@CampanaService, "alert_shake", R.string.alert_shake)
                    val event = Event(
                        timestamp = System.currentTimeMillis(),
                        type = "shake",
                        message = shakeMsg,
                        sourceIp = "Local",
                        sourceTerminalName = sourceTerminalName
                    )
                    db.eventDao().insertEvent(event)
                    FileLogger.logInfo(TAG, "Shake alert logged to local DB")
                } catch (e: Exception) {
                    FileLogger.logCritical(TAG, "Failed to log shake alert to DB", e)
                }
            }

            val monitorIp = prefs.getString("paired_monitor_ip", null)
            if (monitorIp == null) {
                Log.w(TAG, "sendShakeAlert: paired_monitor_ip is null, alert NOT sent to Monitor")
            }
            if (monitorIp != null) {
                Thread {
                    try {
                        val url = URL("http://$monitorIp:8080/alert/shake")
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 3000
                        connection.doOutput = true
                        connection.outputStream.write("status=triggered".toByteArray())
                        val responseCode = connection.responseCode
                        FileLogger.logInfo(TAG, "Shake alert sent to $monitorIp, response: $responseCode")
                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending shake to Monitor", e)
                    }
                }.start()
            }
        } else {
            NotificationHelper.notifyAlert(this, "¡Caída detectada!", "Se ha detectado un movimiento brusco", "shake")
        }
    }

    private fun sendVoiceAlert(keyword: String) {
        if (userRole == ROLE_TERMINAL) {
            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            val monitorIp = prefs.getString("paired_monitor_ip", null)
            if (monitorIp == null) {
                Log.w(TAG, "sendVoiceAlert: paired_monitor_ip is null, alert NOT sent to Monitor")
            }
            if (monitorIp != null) {
                Thread {
                    try {
                        val url = URL("http://$monitorIp:8080/trigger_voice")
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 3000
                        connection.doOutput = true
                        val json = JSONObject().apply { put("text", keyword) }
                        connection.outputStream.write(json.toString().toByteArray())
                        val responseCode = connection.responseCode
                        FileLogger.logInfo(TAG, "Voice alert sent to $monitorIp, response: $responseCode")
                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending voice alert to Monitor", e)
                    }
                }.start()
            }
        } else {
            NotificationHelper.notifyAlert(this, "🎙️ Voz detectada", "Palabra clave: $keyword", "voice")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "UPDATE_ALERTS" -> updateNotification()
            "REFRESH_SERVICES" -> refreshServices()
            else -> handleAudioCallCommand(intent)
        }
        return START_STICKY
    }

    private fun handleAudioCallCommand(intent: Intent?) {
        val action = intent?.action
        val callAction = intent?.getStringExtra("call_action")
        
        if (action == ACTION_TRIGGER_BELL) {
            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            val monitorIp = prefs.getString("paired_monitor_ip", null)
            if (monitorIp == null) {
                Log.w(TAG, "triggerBell: paired_monitor_ip is null, bell NOT sent to Monitor")
            }
            if (monitorIp != null) {
                Thread {
                    try {
                        val url = URL("http://$monitorIp:8080/trigger_bell")
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 3000
                        connection.doOutput = true
                        connection.outputStream.write("type=bell&source=terminal".toByteArray())
                        val responseCode = connection.responseCode
                        FileLogger.logInfo(TAG, "Bell alert sent to $monitorIp, response: $responseCode")
                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending bell to Monitor", e)
                    }
                }.start()
            }
            return
        }

        when (callAction) {
            "start" -> startAudioCall()
            "stop" -> stopAudioCall()
        }
    }

    // T46: Real startAudioCall
    private fun startAudioCall() {
        isAudioCallActive = true
        updateNotification()
        val monitorIp = EncryptedPreferencesHelper.getString(this, "paired_monitor_ip", "")
        if (monitorIp?.isNotEmpty() == true) { // Fixed T46 build error
            callManager = CallManager(monitorIp, 5060)
            callManager?.startCall()
        }
    }

    // T46: Real stopAudioCall
    private fun stopAudioCall() {
        isAudioCallActive = false
        callManager?.stopCall()
        callManager = null
        updateNotification()
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, createServiceNotification())
    }

    private fun createServiceNotification(): Notification {
        val targetActivity = if (userRole == ROLE_MONITOR) MonitorMainActivity::class.java else BellActivity::class.java
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, targetActivity), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, NotificationHelper.SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)

        if (userRole == ROLE_MONITOR) {
            val expandedView = RemoteViews(packageName, R.layout.notification_custom)
            expandedView.setTextViewText(R.id.tvNotifTitle, "Monitor de Cuidados")
            
            val alerts = NotificationHelper.getAlertHistory()
            if (alerts.isEmpty()) {
                expandedView.setTextViewText(R.id.tvNotifLog, if (isAudioCallActive) "📞 Llamada de audio activa" else "Monitor activo")
                expandedView.setViewVisibility(R.id.btnNotifClear, android.view.View.GONE)
            } else {
                val logText = alerts.reversed().joinToString("\n") { "• ${it.title}: ${it.message}" }
                expandedView.setTextViewText(R.id.tvNotifLog, logText)
                expandedView.setViewVisibility(R.id.btnNotifClear, android.view.View.VISIBLE)
            }
            
            if (isAudioCallActive) {
                expandedView.setImageViewResource(R.id.btnNotifCall, R.drawable.ic_call_end)
                expandedView.setOnClickPendingIntent(R.id.btnNotifCall, PendingIntent.getService(this, 20, Intent(this, CampanaService::class.java).apply { putExtra("call_action", "stop") }, PendingIntent.FLAG_IMMUTABLE))
            } else {
                expandedView.setOnClickPendingIntent(R.id.btnNotifCall, PendingIntent.getService(this, 21, Intent(this, CampanaService::class.java).apply { putExtra("call_action", "start") }, PendingIntent.FLAG_IMMUTABLE))
            }
            expandedView.setImageViewResource(R.id.btnNotifVideo, R.drawable.ic_monitor_video)
            expandedView.setOnClickPendingIntent(R.id.btnNotifVideo, PendingIntent.getActivity(this, 11, Intent(this, VideoActivity::class.java).apply { putExtra("mode", "monitor") }, PendingIntent.FLAG_IMMUTABLE))
            expandedView.setOnClickPendingIntent(R.id.btnNotifClear, PendingIntent.getBroadcast(this, 3, Intent(this, com.example.monitordecuidados.receivers.NotificationClearReceiver::class.java), PendingIntent.FLAG_IMMUTABLE))
            
            builder.setCustomBigContentView(expandedView)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            builder.setContentTitle("Campana 24/7")
            builder.setContentText("Servicio Activo")
        }
        return builder.build()
    }

    // T53: checkBatteryLevel
    private fun checkBatteryLevel() {
        val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        if (level <= 15 && lastBatteryAlertState == "ok") {
            lastBatteryAlertState = "low"
            // Enviar HTTP POST al Monitor para que muestre notificación
            val monitorIp = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
                .getString("paired_monitor_ip", "") ?: ""
            if (monitorIp.isNotEmpty()) {
                serviceScope.launch {
                    try {
                        val url = java.net.URL("http://$monitorIp:8080/trigger_bell")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        conn.outputStream.write("{\"message\":\"🔋 Batería baja ($level%)\",\"type\":\"battery_low\"}".toByteArray())
                        conn.responseCode
                        conn.disconnect()
                    } catch (e: Exception) {
                        FileLogger.logWarning(TAG, "Error sending battery_low alert: ${e.message}")
                    }
                }
            }
        } else if (level > 20 && lastBatteryAlertState == "low") {
            lastBatteryAlertState = "ok"
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        // T56: Cancel lockscreen notification to prevent persistence after role switch
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LOCKSCREEN_NOTIFICATION_ID)

        server?.stop()
        voiceCommandManager?.stopListening()
        screenOffReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        sensorManager?.unregisterListener(this)
        // T53: Stop battery monitoring
        batteryCheckHandler.removeCallbacks(batteryCheckRunnable)
        // T55: Unregister network callback
        NetworkUtils.unregisterNetworkCallback(this)
        // T46: End call
        callManager?.stopCall()
    }
}
