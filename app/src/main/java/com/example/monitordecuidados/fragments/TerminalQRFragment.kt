package com.example.monitordecuidados.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.monitordecuidados.CampanaService
import com.example.monitordecuidados.databinding.FragmentTerminalQrBinding
import com.example.monitordecuidados.utils.NetworkUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.util.UUID

class TerminalQRFragment : Fragment() {

    private var _binding: FragmentTerminalQrBinding? = null
    private val binding get() = _binding!!

    private var isUpdatingFromPrefs = false
    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        val b = _binding ?: return@OnSharedPreferenceChangeListener
        val prefs = requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        isUpdatingFromPrefs = true
        when (key) {
            "lockscreen_bell_enabled" -> b.switchBellMode.isChecked = prefs.getBoolean(key, true)
            "shake_detection_enabled" -> b.switchShakeDetection.isChecked = prefs.getBoolean(key, true)
            "voice_detection_enabled" -> b.switchVoiceDetection.isChecked = prefs.getBoolean(key, true)
            "reminders_enabled" -> b.switchAlarms.isChecked = prefs.getBoolean(key, true)
        }
        isUpdatingFromPrefs = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTerminalQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ip = NetworkUtils.getLocalIpAddress() ?: "0.0.0.0"
        binding.tvIpAddress.text = ip
        binding.tvQrInstruction.text = getString(com.example.monitordecuidados.R.string.terminal_scan_qr)
        
        val prefs = requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("terminal_device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("terminal_device_id", deviceId).apply()
        }

        val personName = prefs.getString("terminal_person_name", null)
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
        val displayName = if (!personName.isNullOrEmpty()) personName else "Terminal de ${android.os.Build.MODEL}"
        
        val qrJson = JSONObject().apply {
            put("deviceId", deviceId)
            put("ip", ip)
            put("port", 8080)
            put("name", displayName)
            put("secret", "CampanaSecureKey")
        }
        generateQR(qrJson.toString())

        setupServiceSwitches()
    }

    override fun onResume() {
        super.onResume()
        val prefs = requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        // Re-leer estado actual por si cambió mientras Fragment estaba en pausa
        isUpdatingFromPrefs = true
        binding.switchBellMode.isChecked = prefs.getBoolean("lockscreen_bell_enabled", true)
        binding.switchShakeDetection.isChecked = prefs.getBoolean("shake_detection_enabled", true)
        binding.switchVoiceDetection.isChecked = prefs.getBoolean("voice_detection_enabled", true)
        binding.switchAlarms.isChecked = prefs.getBoolean("reminders_enabled", true)
        isUpdatingFromPrefs = false
    }

    override fun onPause() {
        super.onPause()
        requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private fun setupServiceSwitches() {
        val prefs = requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        
        binding.tvServiceStatusIndicator.text = if (CampanaService.isRunning) "Activo" else "Inactivo"
        binding.tvServiceStatusIndicator.setTextColor(
            if (CampanaService.isRunning) 
                resources.getColor(android.R.color.holo_green_dark, null) 
            else 
                resources.getColor(android.R.color.holo_red_dark, null)
        )

        binding.switchBellMode.isChecked = prefs.getBoolean("lockscreen_bell_enabled", true)
        binding.switchShakeDetection.isChecked = prefs.getBoolean("shake_detection_enabled", true)
        binding.switchVoiceDetection.isChecked = prefs.getBoolean("voice_detection_enabled", true)
        binding.switchAlarms.isChecked = prefs.getBoolean("reminders_enabled", true)

        binding.switchBellMode.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromPrefs) {
                prefs.edit().putBoolean("lockscreen_bell_enabled", isChecked).apply()
                refreshService()
            }
        }

        binding.switchShakeDetection.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromPrefs) {
                prefs.edit().putBoolean("shake_detection_enabled", isChecked).apply()
                refreshService()
            }
        }

        binding.switchVoiceDetection.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromPrefs) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        binding.switchVoiceDetection.isChecked = false
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 2001)
                        return@setOnCheckedChangeListener
                    }
                }
                prefs.edit().putBoolean("voice_detection_enabled", isChecked).apply()
                refreshService()
            }
        }

        binding.switchAlarms.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromPrefs) {
                prefs.edit().putBoolean("reminders_enabled", isChecked).apply()
                refreshService()
            }
        }
    }

    private fun refreshService() {
        val intent = Intent(requireContext(), CampanaService::class.java).apply {
            action = "REFRESH_SERVICES"
        }
        requireContext().startService(intent)
    }

    private fun generateQR(content: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("TerminalQRFragment", "Error generating QR", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
