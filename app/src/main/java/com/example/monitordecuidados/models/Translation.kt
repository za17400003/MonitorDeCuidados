package com.example.monitordecuidados.models

/**
 * Domain model for dynamic translations synced from cloud.
 */
data class Translation(
    val key: String,
    val value: String,
    val language: String
)
