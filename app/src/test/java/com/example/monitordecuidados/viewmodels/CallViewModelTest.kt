package com.example.monitordecuidados.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.monitordecuidados.models.CallState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@ExperimentalCoroutinesApi
class CallViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial call state is Idle`() {
        val viewModel = CallViewModel()
        assertEquals(CallState.Idle, viewModel.callState.value)
    }

    @Test
    fun `initiateCall updates state to Connected`() {
        val viewModel = CallViewModel()
        viewModel.initiateCall("test_user")
        assertTrue(viewModel.callState.value is CallState.Connected)
        assertEquals("test_user", (viewModel.callState.value as CallState.Connected).remoteId)
    }

    @Test
    fun `endCall updates state to Ended`() {
        val viewModel = CallViewModel()
        viewModel.initiateCall("test_user")
        viewModel.endCall("test_reason")
        assertTrue(viewModel.callState.value is CallState.Ended)
        assertEquals("test_reason", (viewModel.callState.value as CallState.Ended).reason)
    }
}
