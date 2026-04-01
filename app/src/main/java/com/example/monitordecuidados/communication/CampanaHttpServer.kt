package com.example.monitordecuidados.communication

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import android.util.Log
import android.util.Base64
import com.example.monitordecuidados.security.SecurityConfig
import java.io.InputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class CampanaHttpServer(port: Int, private val listener: OnServerEventListener) : NanoHTTPD(port) {

    private val rateLimiter = RateLimiter()

    // AES Configuration matching Terminal
    private val AES_ALGORITHM = "AES/CBC/PKCS5Padding"
    // Resolved: Using centralized security config
    private val AES_KEY = SecurityConfig.AES_KEY
    private val AES_IV = SecurityConfig.AES_IV

    interface OnServerEventListener {
        fun onBellTriggered(sourceIp: String)
        fun onVoiceTriggered(text: String, sourceIp: String)
        fun onCallRequested(sourceIp: String)
        fun onMonitorRequested(sourceIp: String)
        fun onCommandReceived(command: String, params: JSONObject)
        fun onPairingConfirmed(sourceIp: String, monitorName: String)
        fun onShakeTriggered(sourceIp: String)
        fun onStatusRequested(): JSONObject
    }

    private fun decryptBody(encryptedText: String): String {
        return try {
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, AES_KEY, AES_IV)
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted)
        } catch (e: Exception) {
            // Log.w("CampanaHttpServer", "Decryption failed, assuming plain text")
            encryptedText
        }
    }

    fun makeSecure(keystoreStream: InputStream, password: CharArray) {
        try {
            val keystore = KeyStore.getInstance("BKS")
            keystore.load(keystoreStream, password)
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keystore, password)
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)
            
            makeSecure(sslContext.serverSocketFactory, null)
            Log.d("CampanaHttpServer", "HTTPS enabled")
        } catch (e: Exception) {
            Log.e("CampanaHttpServer", "Failed to enable HTTPS: ${e.message}")
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        val remoteIp = session.remoteIpAddress ?: "unknown"
        
        Log.d("CampanaHttpServer", "Received $method request for $uri from $remoteIp")

        if (!rateLimiter.isAllowed(remoteIp)) {
            return newFixedLengthResponse(Response.Status.TOO_MANY_REQUESTS, 
                "application/json", 
                "{\"error\": \"Rate limit exceeded\"}")
        }

        // T39-A: GET /status endpoint
        if (method == Method.GET && uri == "/status") {
            val statusJson = listener.onStatusRequested()
            return newFixedLengthResponse(Response.Status.OK, "application/json", statusJson.toString())
        }

        if (method == Method.POST) {
            val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
            if (contentLength > 10 * 1024 * 1024) {
                return newFixedLengthResponse(Response.Status.PAYLOAD_TOO_LARGE, 
                    "application/json", 
                    "{\"error\": \"Payload too large\"}")
            }

            val files = HashMap<String, String>()
            try {
                session.parseBody(files)
                val rawData = files["postData"] ?: ""
                val postData = decryptBody(rawData)
                
                if (postData.isNotEmpty()) {
                    when (uri) {
                        "/", "/trigger_bell" -> {
                            val json = try { JSONObject(postData) } catch (e: Exception) { JSONObject() }
                            val message = json.optString("message", "")
                            if (message.contains("Voz")) {
                                listener.onVoiceTriggered(message.substringAfter(": "), remoteIp)
                            } else if (message.contains("agitación")) {
                                listener.onShakeTriggered(remoteIp)
                            } else {
                                listener.onBellTriggered(remoteIp)
                            }
                            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                        }
                        "/alert/shake" -> {
                            listener.onShakeTriggered(remoteIp)
                            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                        }
                        "/trigger_voice" -> {
                            val json = try { JSONObject(postData) } catch (e: Exception) { JSONObject() }
                            val text = json.optString("text", "")
                            listener.onVoiceTriggered(text, remoteIp)
                            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                        }
                        "/request_call" -> {
                            listener.onCallRequested(remoteIp)
                            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                        }
                        "/request_monitor" -> {
                            listener.onMonitorRequested(remoteIp)
                            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                        }
                        "/confirm_pairing" -> {
                            val json = try { JSONObject(postData) } catch (e: Exception) { JSONObject() }
                            val monitorName = json.optString("monitorName", "Monitor")
                            // T38-D: Use monitorIp from JSON body if exists
                            val monitorIp = json.optString("monitorIp", remoteIp)
                            val effectiveIp = if (monitorIp.isNotEmpty() && monitorIp != "0.0.0.0") monitorIp else remoteIp
                            listener.onPairingConfirmed(effectiveIp, monitorName)
                            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                        }
                        "/command" -> {
                            val json = try { JSONObject(postData) } catch (e: Exception) { JSONObject() }
                            val command = json.optString("command", "")
                            listener.onCommandReceived(command, json)
                            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                        }
                        else -> return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
                    }
                }
            } catch (e: Exception) {
                Log.e("CampanaHttpServer", "Error parsing request: ${e.message}")
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: ${e.message}")
            }
        }
        
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "Monitor de Cuidados Server Running")
    }
}