package com.example.monitordecuidados

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.monitordecuidados.databinding.ActivityQrScannerBinding
import com.example.monitordecuidados.security.CertificatePinning
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.utils.NetworkUtils
import com.example.monitordecuidados.viewmodels.ConnectionViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity for scanning QR codes to pair Monitor and Terminal.
 * Phase 5: Integrated with ConnectionViewModel.
 * T57v2: Synchronous pairing confirmation before navigation.
 */
class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService
    private val httpClient = CertificatePinning.getSecureOkHttpClient()
    private lateinit var scanningOverlay: View
    private var qrDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanningOverlay = binding.scanningOverlay
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnBackQR.setOnClickListener {
            finish()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(
                            onQrCodeDetected = { qrCode -> handleQrCode(qrCode) },
                            scanningOverlay = scanningOverlay
                        ))
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("QRScanner", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleQrCode(qrCode: String) {
        if (qrDetected) return
        
        try {
            val json = JSONObject(qrCode)
            val deviceId = json.getString("deviceId")
            val ip = json.getString("ip")
            val port = json.getInt("port")
            val name = json.optString("name", "Terminal")
            val secret = json.optString("secret", "CampanaSecureKey")

            qrDetected = true

            EncryptedPreferencesHelper.saveString(this, "paired_terminal_id", deviceId)
            EncryptedPreferencesHelper.saveString(this, "paired_terminal_ip", ip)
            EncryptedPreferencesHelper.getPrefs(this).edit().putInt("paired_terminal_port", port).apply()
            EncryptedPreferencesHelper.saveString(this, "paired_terminal_name", name)
            EncryptedPreferencesHelper.saveString(this, "pairing_secret", secret)

            // T40: Sync to regular SharedPrefs for CampanaService PendingIntents
            getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE).edit()
                .putString("paired_terminal_ip", ip)
                .apply()

            connectionViewModel.addPairing(deviceId, name, "internet")
            
            // T57v2: Confirm pairing BEFORE navigating - synchronous with retry
            Thread {
                val pairingSuccess = confirmPairingToTerminalSync(ip, port)
                runOnUiThread {
                    if (pairingSuccess) {
                        Toast.makeText(this, "Terminal vinculada correctamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Vinculada (verificación pendiente)", Toast.LENGTH_LONG).show()
                    }
                    val intent = Intent(this, MonitorMainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }.start()

        } catch (e: Exception) {
            Log.e("QRScanner", "Invalid QR code format", e)
            qrDetected = false // Allow retry on parse error
        }
    }

    private fun confirmPairingToTerminalSync(terminalIp: String, port: Int): Boolean {
        val myIp = NetworkUtils.getLocalIpAddress() ?: "0.0.0.0"
        val myName = android.os.Build.MODEL
        val json = JSONObject().apply {
            put("monitorName", myName)
            put("monitorIp", myIp)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())

        // 3 attempts with backoff: 0s, 1s, 2s
        for (attempt in 1..3) {
            try {
                val url = "http://$terminalIp:$port/confirm_pairing"
                val request = Request.Builder().url(url).post(body).build()
                val response = httpClient.newCall(request).execute() // SÍNCRONO
                val code = response.code
                response.close()
                if (code in 200..299) {
                    Log.d("QRScanner", "Pairing confirmed on attempt $attempt, response: $code")
                    return true
                }
            } catch (e: Exception) {
                Log.w("QRScanner", "Pairing attempt $attempt failed: ${e.message}")
            }
            if (attempt < 3) try { Thread.sleep(attempt * 1000L) } catch (e: Exception) {}
        }
        Log.e("QRScanner", "Pairing confirmation failed after 3 attempts")
        return false
    }

    private class BarcodeAnalyzer(
        private val onQrCodeDetected: (String) -> Unit,
        private val scanningOverlay: View
    ) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            for (barcode in barcodes) {
                                barcode.rawValue?.let {
                                    playDetectionAnimation()
                                    onQrCodeDetected(it)
                                }
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }

        private fun playDetectionAnimation() {
            scanningOverlay.post {
                scanningOverlay.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .alpha(0.7f)
                    .setDuration(150)
                    .withEndAction {
                        scanningOverlay.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(150)
                            .start()
                    }
                    .start()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
