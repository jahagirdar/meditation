package com.serenity.data.db.entities

import androidx.room.*
import com.serenity.domain.model.PranayamaSession
import com.serenity.domain.model.PranayamaTechnique
import java.time.Instant
import java.util.UUID

@Entity(tableName = "pranayama_sessions")
data class PranayamaSessionEntity(
    @PrimaryKey val id: String,
    val technique: String,          // PranayamaTechnique.name()
    val roundsCompleted: Int,
    val totalRounds: Int,
    val durationSec: Int,
    val startedAt: Long,            // epoch millis
    val endedAt: Long,
) {
    fun toDomain() = PranayamaSession(
        id              = UUID.fromString(id),
        technique       = PranayamaTechnique.valueOf(technique),
        roundsCompleted = roundsCompleted,
        totalRounds     = totalRounds,
        durationSec     = durationSec,
        startedAt       = Instant.ofEpochMilli(startedAt),
        endedAt         = Instant.ofEpochMilli(endedAt),
    )

    companion object {
        fun fromDomain(s: PranayamaSession) = PranayamaSessionEntity(
            id              = s.id.toString(),
            technique       = s.technique.name,
            roundsCompleted = s.roundsCompleted,
            totalRounds     = s.totalRounds,
            durationSec     = s.durationSec,
            startedAt       = s.startedAt.toEpochMilli(),
            endedAt         = s.endedAt.toEpochMilli(),
        )
    }
}
