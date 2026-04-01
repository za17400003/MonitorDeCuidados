package com.example.monitordecuidados.communication

import android.annotation.SuppressLint
import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.example.monitordecuidados.logging.FileLogger
import com.example.monitordecuidados.utils.NetworkUtils
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Manages audio calls between Monitor and Terminal.
 * Uses UDP for low-latency transmission.
 *
 * SPEC STABIL-02, DOC-01
 */
class CallManager(@NonNull private val remoteIp: String, private val port: Int) {
    private val TAG = "CallManager"
    private val SAMPLE_RATE = 8000
    private val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    private val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT) * 2

    @Volatile
    private var isCalling = false
    private var socket: DatagramSocket? = null
    private var sendThread: Thread? = null
    private var receiveThread: Thread? = null

    companion object {
        const val KEEP_ALIVE_INTERVAL = 5000L
        const val SOCKET_TIMEOUT = 10000
    }

    private val keepAliveHandler = Handler(Looper.getMainLooper())
    private val keepAliveRunnable = object : Runnable {
        override fun run() {
            if (isCalling) {
                sendKeepAlive()
                keepAliveHandler.postDelayed(this, KEEP_ALIVE_INTERVAL)
            }
        }
    }

    /**
     * Starts the audio call.
     * Initializes sockets and audio threads.
     */
    fun startCall() {
        if (isCalling) return
        isCalling = true
        
        Thread {
            try {
                socket?.close()
                socket = DatagramSocket(port).apply {
                    receiveBufferSize = 1024 * 64
                    reuseAddress = true
                    soTimeout = SOCKET_TIMEOUT
                }
                
                FileLogger.logInfo(TAG, "Llamada iniciada hacia $remoteIp:$port")
                
                sendThread = Thread { sendAudio() }.apply { 
                    name = "AudioSendThread"
                    start() 
                }
                receiveThread = Thread { receiveAudioLoop() }.apply { 
                    name = "AudioReceiveThread"
                    start() 
                }
                
                keepAliveHandler.post(keepAliveRunnable)
                
            } catch (e: Exception) {
                FileLogger.logCritical(TAG, "Error fatal al iniciar sockets de llamada", e)
                Log.e(TAG, "Critical error starting call sockets", e)
                isCalling = false
            }
        }.start()
    }

    private fun sendKeepAlive() {
        Thread {
            try {
                val buffer = "KEEP_ALIVE".toByteArray()
                val address = InetAddress.getByName(remoteIp)
                val packet = DatagramPacket(buffer, buffer.size, address, port)
                socket?.send(packet)
            } catch (e: Exception) {
                FileLogger.logWarning(TAG, "Failed to send keep-alive: ${e.message}")
            }
        }.start()
    }

    /**
     * Stops the audio call and releases resources.
     */
    fun stopCall() {
        FileLogger.logInfo(TAG, "Deteniendo llamada...")
        isCalling = false
        keepAliveHandler.removeCallbacks(keepAliveRunnable)
        
        try {
            socket?.close()
            socket = null
            
            sendThread?.interrupt()
            receiveThread?.interrupt()
            sendThread = null
            receiveThread = null
        } catch (e: Exception) {
            FileLogger.logWarning(TAG, "Error closing call resources: ${e.message}")
            Log.e(TAG, "Error closing call resources", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendAudio() {
        var recorder: AudioRecord? = null
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION, 
                SAMPLE_RATE, 
                CHANNEL_CONFIG_IN, 
                AUDIO_FORMAT, 
                BUFFER_SIZE
            )
            
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                FileLogger.logCritical(TAG, "AudioRecord no se pudo inicializar")
                return
            }

            val buffer = ByteArray(640)
            val address = InetAddress.getByName(remoteIp)
            
            recorder.startRecording()
            
            while (isCalling && !Thread.currentThread().isInterrupted) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0 && socket != null && !socket!!.isClosed) {
                    val packet = DatagramPacket(buffer, read, address, port)
                    socket?.send(packet)
                }
            }
        } catch (e: Exception) {
            FileLogger.logCritical(TAG, "Error en transmisión de audio", e)
            Log.e(TAG, "Error in audio transmission", e)
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing recorder", e)
            }
            FileLogger.logInfo(TAG, "Transmisión finalizada")
        }
    }

    private fun receiveAudioLoop() {
        while (isCalling) {
            try {
                receiveAudio()
            } catch (e: Exception) {
                FileLogger.logWarning(TAG, "Error in receive loop, retrying... ${e.message}")
                Log.e(TAG, "Error in receive loop", e)
                if (isCalling) Thread.sleep(1000)
            }
        }
    }

    private fun receiveAudio() {
        var player: AudioTrack? = null
        try {
            player = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG_OUT)
                    .build())
                .setBufferSizeInBytes(BUFFER_SIZE)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (player.state != AudioTrack.STATE_INITIALIZED) {
                FileLogger.logCritical(TAG, "AudioTrack no se pudo inicializar")
                return
            }

            val buffer = ByteArray(2048)
            val localIp = NetworkUtils.getLocalIpAddress()
            
            player.play()
            
            while (isCalling && !Thread.currentThread().isInterrupted) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket?.receive(packet)
                    
                    if (packet.length == 10 && String(packet.data, 0, 10) == "KEEP_ALIVE") {
                        continue
                    }

                    if (packet.address.hostAddress != localIp) {
                        player.write(packet.data, 0, packet.length)
                    }
                } catch (e: SocketTimeoutException) {
                    FileLogger.logWarning(TAG, "Receive timeout, connection might be lost")
                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving packet", e)
                    break
                }
            }
        } catch (e: Exception) {
            FileLogger.logCritical(TAG, "Error en recepción de audio", e)
            Log.e(TAG, "Error in audio reception", e)
        } finally {
            try {
                player?.stop()
                player?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing player", e)
            }
            FileLogger.logInfo(TAG, "Recepción finalizada")
        }
    }
}
