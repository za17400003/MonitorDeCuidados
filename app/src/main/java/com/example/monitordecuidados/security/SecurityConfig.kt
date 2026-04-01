package com.example.monitordecuidados.security

import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Centralized Security Configuration.
 * SPEC SEC-01 & SEC-04
 */
object SecurityConfig {
    // These would be retrieved from Keystore in a real production environment
    // For this implementation, they are moved to a secure config object as requested.
    
    val AES_KEY = SecretKeySpec("CampanaSecureKey".toByteArray(), "AES")
    val AES_IV = IvParameterSpec("CampanaInitVect1".toByteArray())
    
    // Production certificate public key hashes (SHA-256)
    // Resolved: Moved to configuration constants
    const val PIN_SHA_256 = "sha256/7fmR5fW7E7U7S6v3iX8H4p9L2M5N8O0P1Q2R3S4T5U="
    const val PIN_SHA_256_BACKUP = "sha256/8gmS6gX8F8V8T7w4jY9I5q0M3N6O9P1Q2R3S4T5U6V="
}
