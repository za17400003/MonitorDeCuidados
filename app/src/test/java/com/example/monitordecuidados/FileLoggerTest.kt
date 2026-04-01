package com.example.monitordecuidados

import com.example.monitordecuidados.logging.FileLogger
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for FileLogger.
 * SPEC TEST-01
 */
class FileLoggerTest {
    @Test
    fun testLoggerObject() {
        assertNotNull(FileLogger)
    }
}
