package com.serenity.data.db

import androidx.room.*
import com.serenity.data.db.entities.PranayamaSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PranayamaDao {
    @Query("SELECT * FROM pranayama_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<PranayamaSessionEntity>>

    @Query("SELECT * FROM pranayama_sessions WHERE startedAt >= :fromMillis ORDER BY startedAt DESC")
    fun observeSince(fromMillis: Long): Flow<List<PranayamaSessionEntity>>

    @Query("SELECT COUNT(*) FROM pranayama_sessions")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(durationSec), 0) FROM pranayama_sessions")
    suspend fun totalSeconds(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PranayamaSessionEntity)

    @Delete
    suspend fun delete(session: PranayamaSessionEntity)
}
