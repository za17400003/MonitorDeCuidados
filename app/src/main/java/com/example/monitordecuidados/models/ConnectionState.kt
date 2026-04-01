package com.example.monitordecuidados.models

sealed class ConnectionState {
    object Connected : ConnectionState()
    object Disconnected : ConnectionState()
    object Reconnecting : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
