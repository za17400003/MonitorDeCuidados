package com.example.monitordecuidados.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val message: String,
    val soundUri: String?,
    val repeatMode: String, // "once", "daily", "weekdays"
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",  // NEW: "terminalId" or "monitorId" for attribution
    val terminalId: String? = null  // NEW: Link to Terminal
)
