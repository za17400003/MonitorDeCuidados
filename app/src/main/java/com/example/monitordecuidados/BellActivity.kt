package com.example.monitordecuidados

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.LayoutLockscreenBinding
import com.example.monitordecuidados.utils.NetworkUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.util.UUID

/**
 * Activity for the "Bell" (Terminal) mode.
 * Optimized for Lockscreen (overlay) as per User Feedback.
 * M13/T12: Added QR generation with JSON format.
 */
class BellActivity : AppCompatActivity() {

    private lateinit var binding: LayoutLockscreenBinding
    private val TAG = "BellActivity"
    private var userPresentReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // T56: If role is monitor, this activity should not exist
        val currentRole = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            .getString("user_role", "terminal")
        if (currentRole == "monitor") {
            finish()
            return
        }

        try {
            setupLockscreenFlags()
            
            binding = LayoutLockscreenBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            hideSystemBars()
            
            setupUI()
            handleIntent(intent)

            // Dismiss lockscreen notification if we were launched from it
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(2)  // LOCKSCREEN_NOTIFICATION_ID = 2

            registerUserPresentReceiver()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    private fun setupUI() {
        binding.btnCloseOverlay.setOnClickListener {
            finish()
        }
        binding.ivBellLarge.setOnClickListener {
            triggerBell()
        }
        binding.root.setOnClickListener {
            triggerBell()
        }
        
        generatePairingQR()
    }

    private fun generatePairingQR() {
        val prefs = getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("terminal_device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("terminal_device_id", deviceId).apply()
        }
        val ip = NetworkUtils.getLocalIpAddress() ?: "0.0.0.0"

        val personName = prefs.getString("terminal_person_name", null)
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
        val displayName = if (!personName.isNullOrEmpty()) personName else "Terminal de ${Build.MODEL}"

        val qrJson = JSONObject().apply {
            put("deviceId", deviceId)
            put("ip", ip)
            put("port", 8080)
            put("name", displayName)
            put("secret", "CampanaSecureKey")
        }
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrJson.toString(), BarcodeFormat.QR_CODE, 512, 512)
            val bitmap = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            binding.ivQrPairing.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR", e)
        }
    }

    private var bellMediaPlayer: android.media.MediaPlayer? = null

    private fun playBellSound() {
        try {
            // Release previous if still playing (prevents overlap on rapid taps)
            bellMediaPlayer?.release()

            val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.bell_chime)
            if (mediaPlayer != null) {
                mediaPlayer.setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mediaPlayer.setOnCompletionListener { it.release(); bellMediaPlayer = null }
                bellMediaPlayer = mediaPlayer
                mediaPlayer.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing bell sound", e)
        }
    }

    private fun triggerBell() {
        playBellSound()
        val intent = Intent(this, CampanaService::class.java).apply {
            action = CampanaService.ACTION_TRIGGER_BELL
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun registerUserPresentReceiver() {
        userPresentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_USER_PRESENT) {
                    finish()
                }
            }
        }
        registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    private fun showIncomingCall(remoteIp: String) {
        val intent = Intent(this, VideoActivity::class.java).apply {
            putExtra("mode", "videocall")
            putExtra("remote_ip", remoteIp)
            putExtra("auto_accept", true)
        }
        startActivity(intent)
    }

    private fun showIncomingMonitor(remoteIp: String) {
        val intent = Intent(this, VideoActivity::class.java).apply {
            putExtra("mode", "monitor")
            putExtra("remote_ip", remoteIp)
            putExtra("auto_accept", true)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // T56: Dismiss if role switched to monitor while in background
        val currentRole = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            .getString("user_role", "terminal")
        if (currentRole == "monitor") {
            finish()
            return
        }
        hideSystemBars()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val mode = intent?.getStringExtra("mode")
        val remoteIp = intent?.getStringExtra("remote_ip")
        
        if (mode == "incoming_call" && remoteIp != null) {
            showIncomingCall(remoteIp)
        } else if (mode == "incoming_monitor" && remoteIp != null) {
            showIncomingMonitor(remoteIp)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bellMediaPlayer?.release()
        bellMediaPlayer = null
        userPresentReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
            userPresentReceiver = null
        }
    }
}