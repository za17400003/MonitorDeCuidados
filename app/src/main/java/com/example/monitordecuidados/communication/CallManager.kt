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
import java.nio.ByteBuffer

/**
 * Manages audio calls between Monitor and Terminal.
 * Uses UDP for low-latency transmission.
 * Migrated to OPUS codec in T47.
 */
class CallManager(@NonNull private val remoteIp: String, private val port: Int) {
    private val TAG = "CallManager"
    private val SAMPLE_RATE = 16000 // T47: Increased to 16kHz for Opus
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

    fun startCall() {
        if (isCalling) return
        isCalling = true
        
        Thread {
            try {
                socket?.close()
                // T87: Create unbound socket, set reuseAddress BEFORE binding.
                // DatagramSocket(port) binds immediately — reuseAddress set after has no effect.
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    receiveBufferSize = 1024 * 64
                    soTimeout = SOCKET_TIMEOUT
                    bind(java.net.InetSocketAddress(port))
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
        var encoder: MediaCodec? = null
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION, 
                SAMPLE_RATE, 
                CHANNEL_CONFIG_IN, 
                AUDIO_FORMAT, 
                BUFFER_SIZE
            )
            
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                FileLogger.logCritical(TAG, "T82: AudioRecord no inicializado — ¿RECORD_AUDIO otorgado?")
                Log.e(TAG, "T82: AudioRecord STATE_UNINITIALIZED. Permission RECORD_AUDIO may be missing.")
                return
            }

            // T47/T87: OPUS Encoder setup — log explicit error if codec unavailable
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, 1)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 16000)
            try {
                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
                encoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder!!.start()
            } catch (e: Exception) {
                FileLogger.logCritical(TAG, "T87: OPUS encoder NOT available on this device. Call audio will not work.", e)
                Log.e(TAG, "T87: OPUS encoder creation failed — codec not supported?", e)
                return
            }

            val pcmBuffer = ByteArray(960 * 2) // 60ms at 16kHz
            val address = InetAddress.getByName(remoteIp)
            
            recorder.startRecording()
            val bufferInfo = MediaCodec.BufferInfo()
            
            while (isCalling && !Thread.currentThread().isInterrupted) {
                val read = recorder.read(pcmBuffer, 0, pcmBuffer.size)
                if (read > 0) {
                    val inputIndex = encoder.dequeueInputBuffer(1000)
                    if (inputIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(pcmBuffer, 0, read)
                        encoder.queueInputBuffer(inputIndex, 0, read, System.nanoTime() / 1000, 0)
                    }
                }

                var outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 1000)
                while (outputIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && socket != null && !socket!!.isClosed) {
                        val opusData = ByteArray(bufferInfo.size)
                        outputBuffer.get(opusData)
                        val packet = DatagramPacket(opusData, opusData.size, address, port)
                        socket?.send(packet)
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                }
            }
        } catch (e: Exception) {
            FileLogger.logCritical(TAG, "Error en transmisión de audio (Opus)", e)
            Log.e(TAG, "Error in audio transmission", e)
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
                encoder?.stop()
                encoder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing recorder/encoder", e)
            }
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
        var decoder: MediaCodec? = null
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

            // T47: OPUS Decoder setup
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, 1)
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val udpBuffer = ByteArray(2048)
            val localIp = NetworkUtils.getLocalIpAddress()
            val bufferInfo = MediaCodec.BufferInfo()
            
            player.play()
            
            while (isCalling && !Thread.currentThread().isInterrupted) {
                val packet = DatagramPacket(udpBuffer, udpBuffer.size)
                try {
                    socket?.receive(packet)
                    
                    if (packet.length == 10 && String(packet.data, 0, 10) == "KEEP_ALIVE") {
                        continue
                    }

                    if (packet.address.hostAddress != localIp) {
                        val inputIndex = decoder.dequeueInputBuffer(1000)
                        if (inputIndex >= 0) {
                            val inputBuffer = decoder.getInputBuffer(inputIndex)
                            inputBuffer?.clear()
                            inputBuffer?.put(packet.data, 0, packet.length)
                            decoder.queueInputBuffer(inputIndex, 0, packet.length, System.nanoTime() / 1000, 0)
                        }

                        var outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 1000)
                        while (outputIndex >= 0) {
                            val outputBuffer = decoder.getOutputBuffer(outputIndex)
                            if (outputBuffer != null) {
                                val pcmData = ByteArray(bufferInfo.size)
                                outputBuffer.get(pcmData)
                                player.write(pcmData, 0, pcmData.size)
                            }
                            decoder.releaseOutputBuffer(outputIndex, false)
                            outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 0)
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    FileLogger.logWarning(TAG, "Receive timeout, connection might be lost")
                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving packet", e)
                    break
                }
            }
        } catch (e: Exception) {
            FileLogger.logCritical(TAG, "Error en recepción de audio (Opus)", e)
            Log.e(TAG, "Error in audio reception", e)
        } finally {
            try {
                player?.stop()
                player?.release()
                decoder?.stop()
                decoder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing player/decoder", e)
            }
        }
    }
}
