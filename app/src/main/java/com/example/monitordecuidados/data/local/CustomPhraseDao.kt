package com.example.monitordecuidados.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomPhraseDao {
    @Query("SELECT * FROM custom_phrases ORDER BY created_at DESC")
    fun getAllPhrases(): Flow<List<CustomPhrase>>

    @Query("SELECT * FROM custom_phrases")
    suspend fun getAllPhrasesSync(): List<CustomPhrase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: CustomPhrase)

    @Delete
    suspend fun deletePhrase(phrase: CustomPhrase)
}
