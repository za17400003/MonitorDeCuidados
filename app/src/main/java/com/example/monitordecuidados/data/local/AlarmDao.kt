package com.example.monitordecuidados.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms")
    suspend fun getAllAlarmsSync(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE terminalId = :terminalId ORDER BY hour ASC, minute ASC")
    suspend fun getAlarmsByTerminal(terminalId: String): List<Alarm>

    @Query("SELECT * FROM alarms WHERE terminalId = :terminalId AND enabled = 1 ORDER BY hour ASC")
    suspend fun getActiveAlarmsByTerminal(terminalId: String): List<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): Alarm?
}
