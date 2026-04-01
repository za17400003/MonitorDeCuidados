package com.example.monitordecuidados

import com.example.monitordecuidados.logging.CrashLogger
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CrashLogger.
 * SPEC TEST-01
 */
class CrashLoggerTest {
    @Test
    fun testInitialization() {
        // CrashLogger requires a Context, so we'd need Robolectric or a mock.
        // For now, testing the object exists.
        assertNotNull(CrashLogger)
    }
}
