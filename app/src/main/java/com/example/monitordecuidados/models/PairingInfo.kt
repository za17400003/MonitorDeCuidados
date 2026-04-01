package com.example.monitordecuidados.models

import com.google.firebase.Timestamp

data class PairingInfo(
    val id: String = "",
    val monitorId: String = "",
    val terminalId: String = "",
    val connectionType: String = "internet",
    val lastSeen: Timestamp? = null,
    val name: String? = null,
    val status: String? = null,
    val batteryLevel: Int? = null
)