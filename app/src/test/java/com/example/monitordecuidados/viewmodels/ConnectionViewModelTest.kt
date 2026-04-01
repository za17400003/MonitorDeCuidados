package com.example.monitordecuidados.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.monitordecuidados.models.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class ConnectionViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Test
    fun `initial connection state is Disconnected`() {
        val viewModel = ConnectionViewModel()
        assertEquals(ConnectionState.Disconnected, viewModel.connectionState.value)
    }

    @Test
    fun `updateConnectionStatus updates state correctly`() {
        val viewModel = ConnectionViewModel()
        viewModel.updateConnectionStatus("connected")
        assertEquals(ConnectionState.Connected, viewModel.connectionState.value)
    }
}
