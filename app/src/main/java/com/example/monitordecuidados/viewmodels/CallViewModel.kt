package com.example.monitordecuidados.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monitordecuidados.models.CallState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CallViewModel : ViewModel() {
    private val _callState = MutableLiveData<CallState>(CallState.Idle)
    val callState: LiveData<CallState> = _callState

    private val _callDuration = MutableLiveData<Long>(0)
    val callDuration: LiveData<Long> = _callDuration

    private val _callQuality = MutableLiveData<String>("good")
    val callQuality: LiveData<String> = _callQuality

    private var timerJob: Job? = null

    fun initiateCall(recipientId: String) {
        _callState.value = CallState.Connected(recipientId)
        startTimer()
    }

    fun acceptCall(callerId: String) {
        _callState.value = CallState.Connected(callerId)
        startTimer()
    }

    fun endCall(reason: String? = "user_ended") {
        _callState.value = CallState.Ended(reason)
        stopTimer()
        // Reset to idle after a delay or UI transition
        _callState.postValue(CallState.Idle)
    }

    fun updateCallQuality(quality: String) {
        _callQuality.value = quality
    }

    private fun startTimer() {
        _callDuration.value = 0
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                _callDuration.postValue((System.currentTimeMillis() - startTime) / 1000)
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
