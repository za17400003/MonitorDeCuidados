package com.example.monitordecuidados

import android.Manifest
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.monitordecuidados.communication.CallManager
import com.example.monitordecuidados.communication.VideoManager
import com.example.monitordecuidados.databinding.ActivityVideoBinding
import com.example.monitordecuidados.logging.FileLogger
import com.example.monitordecuidados.models.CallState
import com.example.monitordecuidados.security.CertificatePinning
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.viewmodels.CallViewModel
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket

/**
 * Activity that handles video transmission and reception.
 * Phase 5: Integrated with CallViewModel for state management.
 */
class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding
    private val callViewModel: CallViewModel by viewModels()
    private val client = CertificatePinning.getSecureOkHttpClient()
    private var remoteIp: String? = null
    
    private var videoManager: VideoManager? = null
    private var callManager: CallManager? = null
    
    private var udpSocket: DatagramSocket? = null
    private var isReceiving = false

    private lateinit var gestureDetector: GestureDetector
    private val TAG = "VideoActivity"

    private var currentRol: String = ""
    private var isVideoCallActive: Boolean = false 
    private var isAudioOnly: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            binding = ActivityVideoBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            remoteIp = intent.getStringExtra("remote_ip")
                ?: EncryptedPreferencesHelper.getString(this, "paired_terminal_ip")
            currentRol = prefs.getString("user_role", "monitor") ?: "monitor"

            val mode = intent.getStringExtra("mode") ?: "monitor"
            isVideoCallActive = (mode == "videocall")
            isAudioOnly = (mode == "audiocall")

            if (remoteIp == null && currentRol == "monitor") {
                Toast.makeText(this, getString(R.string.no_terminal_linked), Toast.LENGTH_LONG).show()
                finish()
                return
            }

            setupUI()
            setupGestures()
            setupButtonListeners()
            setupObservers()
            
            lifecycleScope.launch {
                val permissionsValid = validatePermissions()
                if (permissionsValid) {
                    if (currentRol == "monitor") {
                        if (!isAudioOnly) startReceivingVideo()
                        
                        if (isAudioOnly) {
                            startAudioMode()
                        } else if (isVideoCallActive) {
                            callViewModel.initiateCall(remoteIp ?: "unknown")
                            sendCommandToTerminal("ENABLE_AUDIO")
                            sendCommandToTerminal("ENABLE_VIDEO_CALL")
                        }
                    } else {
                        // Terminal side
                        if (intent.getBooleanExtra("auto_accept", false)) {
                            callViewModel.acceptCall(remoteIp ?: "unknown")
                            if (isAudioOnly) {
                                startAudioMode()
                            } else {
                                videoManager?.startStreaming()
                                callManager?.startCall()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            FileLogger.logCritical(TAG, "Error in onCreate", e)
            Log.e(TAG, "Critical error in onCreate", e)
        }
    }

    private fun setupObservers() {
        callViewModel.callState.observe(this) { state ->
            when (state) {
                is CallState.Ended -> {
                    finish()
                }
                is CallState.Connected -> {
                    updateStatusText()
                }
                else -> {}
            }
        }

        callViewModel.callDuration.observe(this) { duration ->
            val minutes = duration / 60
            val seconds = duration % 60
            val timeString = String.format("%02d:%02d", minutes, seconds)
            binding.tvDuration.text = if (isAudioOnly) {
                "${getString(R.string.mode_audio_call)} ($timeString)"
            } else if (isVideoCallActive) {
                "${getString(R.string.mode_video_call)} ($timeString)"
            } else {
                "${getString(R.string.mode_silent_monitor)} ($timeString)"
            }
        }
    }

    private fun startAudioMode() {
        if (callManager == null && remoteIp != null) callManager = CallManager(remoteIp!!, 9000)
        callManager?.startCall()
        callViewModel.initiateCall(remoteIp ?: "unknown")
        if (currentRol == "monitor") {
            sendCommandToTerminal("START_AUDIO_CALL")
        }
        binding.viewRemoteVideo.visibility = View.GONE
    }

    private fun setupUI() {
        if (currentRol == "monitor") {
            binding.cardLocalVideo.visibility = if (isVideoCallActive) View.VISIBLE else View.GONE
            updateStatusText()
            updateToggleButtonStyle()
        } else {
            binding.cardLocalVideo.visibility = View.VISIBLE
            remoteIp?.let { ip ->
                videoManager = VideoManager(this, ip, 9001)
                callManager = CallManager(ip, 9000)
            }
        }
    }

    private fun updateStatusText() {
        binding.tvDuration.text = when {
            isAudioOnly -> getString(R.string.mode_audio_call)
            isVideoCallActive -> getString(R.string.mode_video_call)
            else -> getString(R.string.mode_silent_monitor)
        }
    }

    private fun updateToggleButtonStyle() {
        if (isVideoCallActive) {
            binding.btnToggleMode.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_teal))
            binding.btnToggleMode.setImageResource(R.drawable.ic_monitor_video)
        } else {
            binding.btnToggleMode.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray))
            binding.btnToggleMode.setImageResource(R.drawable.ic_videocall_start)
        }
    }

    private fun setupButtonListeners() {
        binding.btnHangup.setOnClickListener {
            hangUpAll()
        }
        binding.btnToggleMode.setOnClickListener {
            toggleBetweenSilentAndVideoCall()
        }
        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }
    }

    private fun toggleBetweenSilentAndVideoCall() {
        isAudioOnly = false // Toggling exits audio-only if active
        isVideoCallActive = !isVideoCallActive
        if (isVideoCallActive) {
            binding.cardLocalVideo.visibility = View.VISIBLE
            binding.viewRemoteVideo.visibility = View.VISIBLE
            sendCommandToTerminal("ENABLE_AUDIO")
            sendCommandToTerminal("ENABLE_VIDEO_CALL")
            if (callManager == null && remoteIp != null) callManager = CallManager(remoteIp!!, 9000)
            callManager?.startCall()
            callViewModel.initiateCall(remoteIp ?: "unknown")
        } else {
            binding.cardLocalVideo.visibility = View.GONE
            sendCommandToTerminal("DISABLE_AUDIO")
            sendCommandToTerminal("DISABLE_VIDEO_CALL")
            callManager?.stopCall()
            callViewModel.endCall("monitor_toggled_off")
        }
        updateStatusText()
        updateToggleButtonStyle()
    }

    private fun hangUpAll() {
        if (currentRol == "monitor") {
            sendCommandToTerminal("STOP_SESSION")
            if (isAudioOnly) sendCommandToTerminal("STOP_AUDIO_CALL")
        }
        callViewModel.endCall("user_hung_up")
    }

    private fun switchCamera() {
        sendCommandToTerminal("SWITCH_CAMERA")
    }

    private suspend fun validatePermissions(): Boolean {
        return if (currentRol == "monitor") {
            val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            if (audio != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1002)
                false
            } else true
        } else checkLocalPermissions()
    }

    private fun checkLocalPermissions(): Boolean {
        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val granted = camera == PackageManager.PERMISSION_GRANTED && audio == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1001)
        }
        return granted
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 != null && Math.abs(e1.y - e2.y) > 100) {
                    if (e1.y > e2.y) binding.llControls.visibility = View.GONE 
                    else binding.llControls.visibility = View.VISIBLE
                    return true
                }
                return false
            }
        })
        binding.viewRemoteVideo.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.performClick()
            true
        }
    }

    private fun startReceivingVideo() {
        if (isReceiving) return
        isReceiving = true
        Thread {
            try {
                udpSocket = DatagramSocket(9001)
                val buffer = ByteArray(65507)
                while (isReceiving) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    val bitmap = BitmapFactory.decodeByteArray(packet.data, 0, packet.length)
                    if (bitmap != null) {
                        runOnUiThread {
                            val canvas = binding.viewRemoteVideo.lockCanvas()
                            if (canvas != null) {
                                canvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, canvas.width, canvas.height), null)
                                binding.viewRemoteVideo.unlockCanvasAndPost(canvas)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in video receiving loop", e)
            } finally { 
                try {
                    udpSocket?.close() 
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing UDP socket", e)
                }
            }
        }.start()
    }

    private fun sendCommandToTerminal(command: String) {
        if (remoteIp == null) return
        val url = "http://$remoteIp:8080/command"
        val body = JSONObject().apply { put("command", command) }.toString().toRequestBody("application/json".toMediaType())
        client.newCall(Request.Builder().url(url).post(body).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send command $command", e)
            }
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        isReceiving = false
        try {
            udpSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket in onDestroy", e)
        }
        videoManager?.stopStreaming()
        callManager?.stopCall()
    }
}
