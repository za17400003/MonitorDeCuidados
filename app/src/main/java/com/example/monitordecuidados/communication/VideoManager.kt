package com.example.monitordecuidados.communication

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.example.monitordecuidados.logging.FileLogger
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.Executors

/**
 * Manages video streaming from the camera to a remote device.
 * Captures frames, converts them to JPEG, and sends them via UDP.
 *
 * SPEC STABIL-02, DOC-01
 */
class VideoManager(private val context: Context, private val remoteIp: String, private val port: Int) {
    private val TAG = "VideoManager"
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var socket: DatagramSocket? = null
    private var previewSurface: Surface? = null
    
    @Volatile
    private var isStreaming = false
    
    private val executor = Executors.newSingleThreadExecutor()
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    private var currentCameraId: String? = null
    private var lensFacing = CameraCharacteristics.LENS_FACING_FRONT

    companion object {
        /**
         * Finds the front camera ID.
         */
        fun getFrontCameraId(context: Context): String? {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            return manager.cameraIdList.find { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: manager.cameraIdList.firstOrNull()
        }

        /**
         * Finds the back camera ID.
         */
        fun getBackCameraId(context: Context): String? {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            return manager.cameraIdList.find { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }
    }

    /**
     * Starts video streaming.
     * Initializes camera and UDP socket.
     */
    @SuppressLint("MissingPermission")
    fun startStreaming() {
        if (isStreaming && cameraDevice != null) return
        isStreaming = true
        
        FileLogger.logInfo(TAG, "Iniciando streaming de video hacia $remoteIp:$port")
        
        if (currentCameraId == null) {
            currentCameraId = getFrontCameraId(context)
        }
        
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("VideoBackground").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
        
        executor.execute {
            try {
                if (socket == null) {
                    socket = DatagramSocket()
                    socket?.sendBufferSize = 1024 * 1024
                }
                openCamera()
            } catch (e: Exception) {
                FileLogger.logCritical(TAG, "Error starting video streaming", e)
                Log.e(TAG, "Error starting video streaming", e)
            }
        }
    }

    /**
     * Stops video streaming and releases resources.
     */
    fun stopStreaming() {
        FileLogger.logInfo(TAG, "Deteniendo streaming de video")
        isStreaming = false
        backgroundHandler?.post {
            try {
                captureSession?.stopRepeating()
                captureSession?.close()
                cameraDevice?.close()
                imageReader?.close()
                socket?.close()
            } catch (e: Exception) {
                FileLogger.logWarning(TAG, "Error closing camera resources: ${e.message}")
            }
            
            captureSession = null
            cameraDevice = null
            imageReader = null
            socket = null
        }
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
        
        try {
            executor.shutdownNow()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down executor", e)
        }
    }

    /**
     * Switches between front and back camera.
     */
    fun switchCamera() {
        FileLogger.logInfo(TAG, "Cambiando cámara")
        backgroundHandler?.post {
            val newCameraId = if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                getBackCameraId(context) ?: getFrontCameraId(context)
            } else {
                getFrontCameraId(context)
            }
            
            if (newCameraId != null && newCameraId != currentCameraId) {
                currentCameraId = newCameraId
                closeCamera()
                openCamera()
            }
        }
    }

    private fun closeCamera() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            cameraDevice?.close()
            imageReader?.close()
        } catch (e: Exception) {
            FileLogger.logWarning(TAG, "Error during closeCamera: ${e.message}")
        }
        captureSession = null
        cameraDevice = null
        imageReader = null
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val camId = currentCameraId ?: return
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(camId)
            lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: CameraCharacteristics.LENS_FACING_FRONT

            manager.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    FileLogger.logWarning(TAG, "Cámara desconectada")
                    camera.close()
                    cameraDevice = null
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    FileLogger.logCritical(TAG, "Error de cámara: $error")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            FileLogger.logCritical(TAG, "Error al acceder a la cámara", e)
        }
    }

    private fun createCaptureSession() {
        if (cameraDevice == null) return
        
        imageReader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = try { reader.acquireLatestImage() } catch (e: Exception) { 
                Log.e(TAG, "Error acquiring image", e)
                null 
            }
            if (image != null) {
                if (isStreaming) {
                    val bytes = imageToJpeg(image)
                    if (bytes != null) sendPacket(bytes)
                }
                image.close()
            }
        }, backgroundHandler)

        val targets = mutableListOf<Surface>()
        imageReader?.surface?.let { targets.add(it) }
        previewSurface?.let { targets.add(it) }

        try {
            cameraDevice?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    try {
                        val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        imageReader?.surface?.let { builder.addTarget(it) }
                        previewSurface?.let { builder.addTarget(it) }
                        session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                    } catch (e: Exception) {
                        FileLogger.logCritical(TAG, "Error in setRepeatingRequest", e)
                    }
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    FileLogger.logCritical(TAG, "Configuración de sesión de captura fallida")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            FileLogger.logCritical(TAG, "Error al crear sesión de captura", e)
        }
    }

    private fun imageToJpeg(image: Image): ByteArray? {
        try {
            val width = image.width
            val height = image.height
            val planes = image.planes
            if (planes.size < 3) return null

            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val yRowStride = yPlane.rowStride
            val vRowStride = vPlane.rowStride
            val vPixelStride = vPlane.pixelStride
            val uRowStride = uPlane.rowStride
            val uPixelStride = uPlane.pixelStride

            val nv21 = ByteArray(width * height * 3 / 2)
            var pos = 0
            
            for (row in 0 until height) {
                yBuffer.position(row * yRowStride)
                yBuffer.get(nv21, pos, width)
                pos += width
            }

            for (row in 0 until height / 2) {
                for (col in 0 until width / 2) {
                    val vIndex = row * vRowStride + col * vPixelStride
                    val uIndex = row * uRowStride + col * uPixelStride
                    nv21[pos++] = vBuffer.get(vIndex)
                    nv21[pos++] = uBuffer.get(uIndex)
                }
            }

            val rotationNeeded = if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) 270 else 90
            val rotatedNv21 = rotateNV21(nv21, width, height, rotationNeeded)
            
            val out = ByteArrayOutputStream()
            val yuvImage = YuvImage(rotatedNv21, ImageFormat.NV21, height, width, null)
            yuvImage.compressToJpeg(Rect(0, 0, height, width), 70, out)
            return out.toByteArray()
        } catch (e: Exception) {
            FileLogger.logWarning(TAG, "Error converting image to Jpeg: ${e.message}")
            Log.e(TAG, "Error converting image to Jpeg", e)
            return null
        }
    }
    
    private fun rotateNV21(input: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
        val output = ByteArray(input.size)
        val is90or270 = rotation == 90 || rotation == 270
        val outWidth = if (is90or270) height else width
        val outHeight = if (is90or270) width else height
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val srcPos = y * width + x
                var destX = x
                var destY = y
                when (rotation) {
                    90 -> { destX = height - 1 - y; destY = x }
                    180 -> { destX = width - 1 - x; destY = height - 1 - y }
                    270 -> { destX = y; destY = width - 1 - x }
                }
                output[destY * outWidth + destX] = input[srcPos]
            }
        }
        
        val uvHeight = height / 2
        val uvWidth = width / 2
        val offset = width * height
        for (y in 0 until uvHeight) {
            for (x in 0 until uvWidth) {
                val srcPos = offset + (y * uvWidth + x) * 2
                var destX = x
                var destY = y
                when (rotation) {
                    90 -> { destX = uvHeight - 1 - y; destY = x }
                    180 -> { destX = uvWidth - 1 - x; destY = uvHeight - 1 - y }
                    270 -> { destX = y; destY = width - 1 - x }
                }
                val destPos = offset + (destY * (if (is90or270) uvHeight else uvWidth) + destX) * 2
                output[destPos] = input[srcPos]
                output[destPos + 1] = input[srcPos + 1]
            }
        }
        return output
    }

    private fun sendPacket(data: ByteArray) {
        if (!executor.isShutdown) {
            executor.execute {
                if (!isStreaming) return@execute
                try {
                    val currentSocket = socket ?: return@execute
                    val address = InetAddress.getByName(remoteIp)
                    if (data.size < 65507) {
                        val packet = DatagramPacket(data, data.size, address, port)
                        currentSocket.send(packet)
                    }
                } catch (e: SocketException) {
                    FileLogger.logWarning(TAG, "Socket error in sendPacket: ${e.message}")
                } catch (e: UnknownHostException) {
                    FileLogger.logCritical(TAG, "Invalid remoteIp=$remoteIp", e)
                } catch (e: Exception) {
                    FileLogger.logCritical(TAG, "Unexpected error in sendPacket", e)
                    Log.e(TAG, "Unexpected error in sendPacket", e)
                }
            }
        }
    }
}
