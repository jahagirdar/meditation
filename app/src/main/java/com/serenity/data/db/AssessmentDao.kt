package com.serenity.data.db

import androidx.room.*
import com.serenity.data.db.entities.AssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {

    @Query("SELECT * FROM assessments WHERE date = :date LIMIT 1")
    fun observeForDate(date: String): Flow<AssessmentEntity?>

    @Query("SELECT * FROM assessments WHERE date >= :from AND date <= :to ORDER BY date DESC")
    fun observeRange(from: String, to: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments ORDER BY date DESC")
    fun observeAll(): Flow<List<AssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AssessmentEntity)

    @Query("DELETE FROM assessments WHERE date = :date")
    suspend fun deleteForDate(date: String)
}
