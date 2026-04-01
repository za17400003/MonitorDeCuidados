package com.example.monitordecuidados.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "type") val type: String, // "bell", "voice", "shake", "battery_low", "battery_ok", "alarm"
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "source_ip") val sourceIp: String?,
    @ColumnInfo(name = "audio_data") val audioData: ByteArray? = null,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "source_terminal_id") val sourceTerminalId: String = "",
    @ColumnInfo(name = "source_terminal_name") val sourceTerminalName: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Event
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id
    }
}