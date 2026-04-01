package com.example.monitordecuidados

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.ActivityTerminalDetailBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
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
    private var isUpdatingFromServer = false
    private val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            queryTerminalStatus()
            pollHandler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val terminalName = intent.getStringExtra("terminal_name") ?: "Terminal"
        val terminalStatus = intent.getStringExtra("terminal_status") ?: "disconnected"
        remoteIp = EncryptedPreferencesHelper.getString(this, "paired_terminal_ip")

        // Toolbar con back
        binding.toolbar.title = terminalName
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Card principal — nombre + status
        binding.tvTerminalName.text = terminalName
        updateStatusText(terminalStatus)

        // Llamar → VideoActivity mode="audiocall"
        binding.btnCall.setOnClickListener {
            startActivity(Intent(this, VideoActivity::class.java).apply {
                putExtra("mode", "audiocall")
                putExtra("remote_ip", remoteIp)
            })
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

        // T39-C: Polling de estado real arranca en onResume()
    }

    override fun onResume() {
        super.onResume()
        pollHandler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
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
        if (remoteIp == null) {
            setupDefaultSwitchStates()
            return
        }
        val url = "http://$remoteIp:8080/status"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to query terminal status", e)
                runOnUiThread { setupDefaultSwitchStates() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
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
        if (remoteIp == null) return
        val url = "http://$remoteIp:8080/command"
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
