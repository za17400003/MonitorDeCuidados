package com.example.monitordecuidados.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE synced = 0")
    suspend fun getUnsyncedEvents(): List<Event>

    @Query("UPDATE events SET synced = 1 WHERE id = :eventId")
    suspend fun markSynced(eventId: Int)

    @Insert
    suspend fun insertEvent(event: Event): Long

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    /**
     * Retrieves recent events of a specific type.
     * Used for deduplication.
     */
    @Query("SELECT * FROM events WHERE timestamp > :sinceTimestamp AND type = :eventType")
    suspend fun getRecentEventsByType(sinceTimestamp: Long, eventType: String): List<Event>
}
