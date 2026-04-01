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
 * M5 Fix: Use View instead of ScanningOverlay.
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

            // T32-FIX: Navegación INMEDIATA independiente de Firestore write.
            // SharedPreferences ya están guardadas. Firestore es fire-and-forget.
            // El snapshotListener en DashboardFragment se actualizará automáticamente cuando aparezca el documento.
            connectionViewModel.addPairing(deviceId, name, "internet")
            
            runOnUiThread {
                Toast.makeText(this, "Terminal vinculada correctamente", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MonitorMainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            
            // Phase 5: Fire-and-forget background pairing confirmation
            Thread {
                confirmPairingToTerminal(ip, port)
            }.start()

        } catch (e: Exception) {
            Log.e("QRScanner", "Invalid QR code format", e)
            qrDetected = false // Allow retry on parse error
        }
    }

    private fun confirmPairingToTerminal(terminalIp: String, port: Int) {
        val myIp = NetworkUtils.getLocalIpAddress() ?: "0.0.0.0"
        val myName = android.os.Build.MODEL
        
        // T38-E: Use HTTP first for pairing confirmation as Terminal server is HTTP.
        val url = "http://$terminalIp:$port/confirm_pairing"
        val json = JSONObject().apply {
            put("monitorName", myName)
            put("monitorIp", myIp)
        }
        
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Fallback to HTTPS just in case
                val httpsUrl = "https://$terminalIp:$port/confirm_pairing"
                val httpsRequest = Request.Builder().url(httpsUrl).post(body).build()
                httpClient.newCall(httpsRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("QRScanner", "Failed to confirm pairing (HTTP+HTTPS): ${e.message}")
                    }
                    override fun onResponse(call: Call, response: Response) { handleResponse(response) }
                })
            }

            override fun onResponse(call: Call, response: Response) {
                handleResponse(response)
            }
        })
    }
    
    private fun handleResponse(response: Response) {
        Log.d("QRScanner", "Pairing confirmation response: ${response.code}")
        response.close()
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
