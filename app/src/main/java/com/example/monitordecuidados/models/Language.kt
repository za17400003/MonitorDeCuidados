package com.example.monitordecuidados.models

/**
 * Domain model representing a supported language in the system.
 */
data class Language(
    val code: String,
    val name: String,
    val nativeName: String
)
