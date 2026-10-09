package com.example.monitordecuidados.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): LiveData<List<Event>>

    /**
     * T84: Retorna el evento más reciente por cada source_ip (= 1 fila por terminal).
     * Usado por DashboardFragment para mostrar 1 card por terminal.
     */
    @Query("""
        SELECT e.* FROM events e
        INNER JOIN (
            SELECT source_ip, MAX(timestamp) as maxTs
            FROM events
            WHERE type IN ('bell', 'shake', 'voice')
            GROUP BY source_ip
        ) grouped ON e.source_ip = grouped.source_ip AND e.timestamp = grouped.maxTs
        ORDER BY e.timestamp DESC
        LIMIT :limit
    """)
    fun getGroupedAlertsByTerminal(limit: Int = 20): LiveData<List<Event>>

    /**
     * T84: Cuenta total de alertas por source_ip (para badge).
     */
    @Query("SELECT COUNT(*) FROM events WHERE source_ip = :sourceIp AND type IN ('bell', 'shake', 'voice')")
    fun getAlertCountByTerminal(sourceIp: String): LiveData<Int>

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
