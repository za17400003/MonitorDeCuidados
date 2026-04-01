package com.example.monitordecuidados.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_phrases")
data class CustomPhrase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "audio_data") val audioData: ByteArray?,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "encrypted_content") val encryptedContent: String = "",  // NEW: Base64-encoded AES-256 encrypted content
    @ColumnInfo(name = "auto_added") val autoAdded: Boolean = false,     // NEW: true if auto-added when Terminal paired with Monitor
    @ColumnInfo(name = "added_by_monitor_id") val addedBy_monitorId: String? = null  // NEW: if autoAdded, which Monitor added it
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CustomPhrase
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id
    }
}
