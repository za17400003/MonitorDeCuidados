package com.example.monitordecuidados.models

data class UserPreferences(
    val autoAcceptCalls: Boolean = false,
    val enableShakeDetection: Boolean = true,
    val notificationSound: Boolean = true
)
