package com.serenity.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.serenity.data.db.entities.AssessmentEntity
import com.serenity.data.db.entities.PresetEntity
import com.serenity.data.db.entities.PranayamaSessionEntity
import com.serenity.data.db.entities.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        PresetEntity::class,
        AssessmentEntity::class,
        PranayamaSessionEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun presetDao(): PresetDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun pranayamaDao(): PranayamaDao
}
