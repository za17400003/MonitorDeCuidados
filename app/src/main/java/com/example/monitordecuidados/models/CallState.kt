package com.example.monitordecuidados.models

sealed class CallState {
    object Idle : CallState()
    data class Ringing(val callerId: String) : CallState()
    data class Connected(val remoteId: String) : CallState()
    data class Ended(val reason: String?) : CallState()
}
