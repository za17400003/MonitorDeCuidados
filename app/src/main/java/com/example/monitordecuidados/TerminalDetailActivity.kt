package com.example.monitordecuidados

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.ActivityTerminalDetailBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.communication.CallManager
import com.example.monitordecuidados.utils.NetworkUtils
import com.example.monitordecuidados.logging.FileLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class TerminalDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TerminalDetailActivity"
    }

    private lateinit var binding: ActivityTerminalDetailBinding
    private val client = OkHttpClient()
    private var remoteIp: String? = null
    private var terminalId: String? = null
    private var isUpdatingFromServer = false
    private val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            queryTerminalStatus()
            pollHandler.postDelayed(this, 5000)
        }
    }

    private var callManager: CallManager? = null
    private var isCallActive = false
    private var currentCallDocId: String? = null

    private var consecutiveFailures = 0
    private var isConnectionLost = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val terminalName = intent.getStringExtra("terminal_name") ?: "Terminal"
        val terminalStatus = intent.getStringExtra("terminal_status") ?: "disconnected"
        terminalId = intent.getStringExtra("terminal_id")
        remoteIp = EncryptedPreferencesHelper.getString(this, "paired_terminal_ip")

        // Toolbar con back
        binding.toolbar.title = terminalName
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Card principal — nombre + status
        binding.tvTerminalName.text = terminalName
        updateStatusText(terminalStatus)

        // T80: Auto-call cuando viene desde menú radial de burbuja
        val autoCall = intent.getBooleanExtra("auto_call", false)

        // T45: Walkie-talkie in-place
        binding.btnCall.setOnClickListener {
            val ip = remoteIp ?: return@setOnClickListener
            
            if (!isCallActive) {
                // T64: Verificar RECORD_AUDIO antes de iniciar llamada
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(
                        this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 3001
                    )
                    return@setOnClickListener
                }

                // T58: Señalar al Terminal que inicie su CallManager
                Thread {
                    try {
                        val url = java.net.URL("http://$ip:8080/request_call")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.connectTimeout = 3000
                        conn.doOutput = true
                        conn.outputStream.write("source=monitor".toByteArray())
                        conn.responseCode
                        conn.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error notifying terminal of call", e)
                    }
                }.start()

                // Iniciar llamada walkie-talkie in-place lado Monitor
                // T87: Puerto 9050
                callManager = CallManager(ip, 9050)
                callManager?.startCall()
                isCallActive = true
                binding.tvCallLabel.text = "Colgar"
                binding.ivCallIcon.setBackgroundResource(R.drawable.button_rounded_red)
                binding.ivCallIcon.setImageResource(R.drawable.ic_call_end)

                // T48: Implement call_history
                val callDoc = hashMapOf(
                    "caller_id" to FirebaseAuth.getInstance().currentUser?.uid,
                    "receiver_id" to terminalId,
                    "start_time" to FieldValue.serverTimestamp(),
                    "end_time" to null,
                    "duration_seconds" to 0L,
                    "call_type" to "voice",
                    "status" to "in_progress"
                )
                val callRef = FirebaseFirestore.getInstance().collection("call_history").document()
                currentCallDocId = callRef.id
                callRef.set(callDoc)
            } else {
                // T58: Señalar al Terminal que termine su CallManager
                Thread {
                    try {
                        val url = java.net.URL("http://$ip:8080/command")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.connectTimeout = 3000
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        conn.outputStream.write("{\"command\":\"STOP_AUDIO_CALL\"}".toByteArray())
                        conn.responseCode
                        conn.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error notifying terminal to stop call", e)
                    }
                }.start()

                // Colgar lado Monitor
                callManager?.stopCall()
                callManager = null
                isCallActive = false
                binding.tvCallLabel.text = "Llamar"
                binding.ivCallIcon.setBackgroundResource(R.drawable.bg_circle_teal)
                binding.ivCallIcon.setImageResource(R.drawable.ic_call_up)

                // T48: Update call_history
                currentCallDocId?.let { docId ->
                    FirebaseFirestore.getInstance().collection("call_history").document(docId)
                        .update(
                            "end_time", FieldValue.serverTimestamp(),
                            "status", "completed"
                        )
                    currentCallDocId = null
                }
            }
        }

        // Monitorear → VideoActivity mode="monitor"
        binding.btnMonitor.setOnClickListener {
            startActivity(Intent(this, VideoActivity::class.java).apply {
                putExtra("mode", "monitor")
                putExtra("remote_ip", remoteIp)
            })
        }

        // --- Card servicios: control remoto via HTTP ---
        // Estado indicador (default: desconocido hasta que se conecte)
        binding.tvServiceStatusIndicator.text = if (terminalStatus == "connected")
            getString(R.string.service_status_active) else "—"

        // Switches — cada uno envía comando HTTP al Terminal (solo por toque del usuario)
        binding.switchBellMode.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromServer) sendCommandToTerminal(if (isChecked) "SET_BELL_MODE_ON" else "SET_BELL_MODE_OFF")
        }
        binding.switchShakeDetection.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromServer) sendCommandToTerminal(if (isChecked) "SET_SHAKE_ON" else "SET_SHAKE_OFF")
        }
        binding.switchVoiceDetection.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromServer) sendCommandToTerminal(if (isChecked) "SET_VOICE_ON" else "SET_VOICE_OFF")
        }
        binding.switchAlarms.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromServer) sendCommandToTerminal(if (isChecked) "SET_ALARMS_ON" else "SET_ALARMS_OFF")
        }

        // T80: Si auto_call=true, simular tap en Llamar
        if (autoCall) {
            binding.btnCall.post { binding.btnCall.performClick() }
        }
    }

    // T64: Manejar resultado de permiso RECORD_AUDIO
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 3001 && grantResults.isNotEmpty()
            && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Permiso concedido — simular tap en btnCall para reiniciar flujo
            binding.btnCall.performClick()
        }
    }

    override fun onResume() {
        super.onResume()
        pollHandler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        callManager?.stopCall()
    }

    private fun updateStatusText(status: String, battery: Int = -1) {
        val statusLabel = when (status) {
            "connected", "Conectado" -> "🟢 Activo"
            "reconnecting" -> "🟡 Reconectando"
            else -> "🔴 Desconectado"
        }
        binding.tvTerminalStatus.text = if (battery >= 0) "$statusLabel • Batería: $battery%" else statusLabel
    }

    private fun queryTerminalStatus() {
        val ip = remoteIp
        if (ip == null) {
            setupDefaultSwitchStates()
            return
        }
        val url = "http://$ip:8080/status"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to query terminal status", e)
                
                // T54: Reconexión con exponential backoff
                consecutiveFailures++
                val backoffMs = when {
                    consecutiveFailures <= 1 -> 5000L
                    consecutiveFailures <= 2 -> 10000L
                    consecutiveFailures <= 3 -> 30000L
                    else -> 60000L
                }
                pollHandler.removeCallbacks(pollRunnable)
                pollHandler.postDelayed(pollRunnable, backoffMs)

                if (consecutiveFailures * 5000 > 300000) { // >5 min de fallos
                    runOnUiThread {
                        binding.tvServiceStatusIndicator.text = "⚠️ Conexión perdida"
                        binding.tvServiceStatusIndicator.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
                    }
                    isConnectionLost = true
                }
                
                // T55: Internet fallback
                if (consecutiveFailures > 6 && NetworkUtils.getConnectionType() == "local") {
                    if (NetworkUtils.isInternetAvailable(this@TerminalDetailActivity)) {
                        NetworkUtils.setConnectionType("internet", this@TerminalDetailActivity)
                        FileLogger.logInfo("TerminalDetail", "Switched to internet fallback")
                    }
                }

                runOnUiThread { setupDefaultSwitchStates() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        // T54: Reset failures on success
                        consecutiveFailures = 0
                        if (isConnectionLost) {
                            isConnectionLost = false
                            runOnUiThread {
                                binding.tvServiceStatusIndicator.text = "✅ Conectado"
                                binding.tvServiceStatusIndicator.setTextColor(resources.getColor(R.color.primary, theme))
                            }
                            // Restaurar polling normal
                            pollHandler.removeCallbacks(pollRunnable)
                            pollHandler.postDelayed(pollRunnable, 5000)
                        }
                        
                        // T55: Switch back to local if we were in internet
                        if (NetworkUtils.getConnectionType() == "internet") {
                            NetworkUtils.setConnectionType("local", this@TerminalDetailActivity)
                            FileLogger.logInfo("TerminalDetail", "Switched back to local")
                        }

                        val json = JSONObject(it.body?.string() ?: "{}")
                        runOnUiThread {
                            isUpdatingFromServer = true
                            binding.switchBellMode.isChecked = json.optBoolean("bellEnabled", true)
                            binding.switchShakeDetection.isChecked = json.optBoolean("shakeEnabled", true)
                            binding.switchVoiceDetection.isChecked = json.optBoolean("voiceEnabled", true)
                            binding.switchAlarms.isChecked = json.optBoolean("alarmsEnabled", true)
                            isUpdatingFromServer = false
                            
                            val batteryLevel = json.optInt("batteryLevel", -1)
                            val status = json.optString("status", "connected")
                            updateStatusText(status, batteryLevel)
                            binding.tvServiceStatusIndicator.text = getString(R.string.service_status_active)
                        }
                    } else {
                        runOnUiThread { setupDefaultSwitchStates() }
                    }
                }
            }
        })
    }

    private fun setupDefaultSwitchStates() {
        isUpdatingFromServer = true
        binding.switchBellMode.isChecked = true
        binding.switchShakeDetection.isChecked = true
        binding.switchVoiceDetection.isChecked = true
        binding.switchAlarms.isChecked = true
        isUpdatingFromServer = false
    }

    private fun sendCommandToTerminal(command: String) {
        val ip = remoteIp ?: return
        val url = "http://$ip:8080/command"
        val body = JSONObject().apply { put("command", command) }
            .toString().toRequestBody("application/json".toMediaType())
        client.newCall(Request.Builder().url(url).post(body).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to send command $command", e)
                }
                override fun onResponse(call: Call, response: Response) { response.close() }
            })
    }
}
