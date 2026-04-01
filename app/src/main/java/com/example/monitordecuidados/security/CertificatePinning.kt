package com.example.monitordecuidados.security

import android.util.Log
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

/**
 * Security layer for Certificate Pinning.
 * Replaces unsafe TrustManager to prevent MITM attacks.
 *
 * SPEC SEC-01
 */
object CertificatePinning {
    
    /**
     * Returns a secure OkHttpClient with certificate pinning enabled.
     * @return [OkHttpClient] instance
     */
    @JvmStatic
    fun getSecureOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .certificatePinner(
                CertificatePinner.Builder()
                    .add("*.monitordecuidados.local", SecurityConfig.PIN_SHA_256)
                    .add("*.monitordecuidados.local", SecurityConfig.PIN_SHA_256_BACKUP)
                    .build()
            )
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                try {
                    val connection = chain.connection()
                    Log.d("TLS", "Request: ${request.url} over ${connection?.protocol() ?: "unknown"}")
                } catch (e: Exception) {
                    Log.w("TLS", "Could not log connection protocol", e)
                }
                chain.proceed(request)
            }
            .build()
    }
}
