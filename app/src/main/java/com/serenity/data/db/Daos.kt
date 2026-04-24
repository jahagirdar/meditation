package com.serenity.data.db

import androidx.room.*
import com.serenity.data.db.entities.PresetEntity
import com.serenity.data.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getById(id: String): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: PresetEntity)

    @Delete
    suspend fun delete(preset: PresetEntity)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startedAt >= :fromMillis ORDER BY startedAt DESC")
    fun observeSince(fromMillis: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("UPDATE sessions SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String)

    @Delete
    suspend fun delete(session: SessionEntity)

    /** Total minutes meditated (all time) */
    @Query("SELECT COALESCE(SUM(actualDurationSec), 0) FROM sessions")
    suspend fun totalSeconds(): Int

    /** Sessions in last N days — used for streak / stats */
    @Query("""
        SELECT startedAt / 86400000 as dayBucket, SUM(actualDurationSec) as secs
        FROM sessions
        WHERE startedAt >= :fromMillis
        GROUP BY dayBucket
        ORDER BY dayBucket DESC
    """)
    suspend fun dailyBuckets(fromMillis: Long): List<DailyBucket>

    @Query("SELECT SUM(actualDurationSec) FROM sessions WHERE startedAt >= :dayStartMillis")
    suspend fun secondsToday(dayStartMillis: Long): Int
}

data class DailyBucket(
    val dayBucket: Long,
    val secs: Int,
)
