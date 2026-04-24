package com.serenity.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.serenity.domain.model.*
import java.time.Instant
import java.util.UUID

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val durationSec: Int,
    val warmupSec: Int,
    val cooldownSec: Int,
    val intervalSeconds: Int?,        // null = off
    val startBell: String,
    val intervalBell: String,
    val endBell: String,
    val ambientSound: String,
    val silentMode: Boolean,
    val sortOrder: Int,
) {
    fun toDomain() = Preset(
        id = UUID.fromString(id),
        name = name,
        durationSec = durationSec,
        warmupSec = warmupSec,
        cooldownSec = cooldownSec,
        intervalOption = intervalSeconds?.let { sec ->
            IntervalOption.entries.firstOrNull { it.seconds == sec }
        },
        startBell = BellSound.valueOf(startBell),
        intervalBell = BellSound.valueOf(intervalBell),
        endBell = BellSound.valueOf(endBell),
        ambientSound = AmbientSound.valueOf(ambientSound),
        silentMode = silentMode,
        sortOrder = sortOrder,
    )

    companion object {
        fun fromDomain(p: Preset) = PresetEntity(
            id = p.id.toString(),
            name = p.name,
            durationSec = p.durationSec,
            warmupSec = p.warmupSec,
            cooldownSec = p.cooldownSec,
            intervalSeconds = p.intervalOption?.seconds,
            startBell = p.startBell.name,
            intervalBell = p.intervalBell.name,
            endBell = p.endBell.name,
            ambientSound = p.ambientSound.name,
            silentMode = p.silentMode,
            sortOrder = p.sortOrder,
        )
    }
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,      // epoch millis
    val endedAt: Long,
    val plannedDurationSec: Int,
    val actualDurationSec: Int,
    val presetName: String?,
    val warmupSec: Int,
    val cooldownSec: Int,
    val intervalSec: Int?,
    val bellSound: String,
    val ambientSound: String,
    val silentMode: Boolean,
    val notes: String?,
) {
    fun toDomain() = Session(
        id = UUID.fromString(id),
        startedAt = Instant.ofEpochMilli(startedAt),
        endedAt = Instant.ofEpochMilli(endedAt),
        plannedDurationSec = plannedDurationSec,
        actualDurationSec = actualDurationSec,
        presetName = presetName,
        warmupSec = warmupSec,
        cooldownSec = cooldownSec,
        intervalSec = intervalSec,
        bellSound = bellSound,
        ambientSound = ambientSound,
        silentMode = silentMode,
        notes = notes,
    )

    companion object {
        fun fromDomain(s: Session) = SessionEntity(
            id = s.id.toString(),
            startedAt = s.startedAt.toEpochMilli(),
            endedAt = s.endedAt.toEpochMilli(),
            plannedDurationSec = s.plannedDurationSec,
            actualDurationSec = s.actualDurationSec,
            presetName = s.presetName,
            warmupSec = s.warmupSec,
            cooldownSec = s.cooldownSec,
            intervalSec = s.intervalSec,
            bellSound = s.bellSound,
            ambientSound = s.ambientSound,
            silentMode = s.silentMode,
            notes = s.notes,
        )
    }
}
