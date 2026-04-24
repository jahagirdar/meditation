package com.serenity.domain.model

import java.time.Instant
import java.util.UUID

// ──────────────────────────────────────────────
// Enumerations
// ──────────────────────────────────────────────

enum class BellSound(val displayName: String, val rawRes: String) {
    TIBETAN_BOWL("Tibetan Singing Bowl", "bell_tibetan_bowl"),
    TEMPLE_GONG("Temple Gong",           "bell_temple_gong"),
    CRYSTAL_BOWL("Crystal Bowl",         "bell_crystal_bowl"),
    SOFT_CHIME("Soft Chime",             "bell_soft_chime"),
    ZEN_BELL("Zen Bell",                 "bell_zen"),
    RAIN_STICK("Rain Stick",             "bell_rain_stick"),
    WOODEN_BLOCK("Wooden Block",         "bell_wooden_block"),
    SILENT("Silent",                     ""),
}

enum class AmbientSound(val displayName: String, val rawRes: String) {
    NONE("None",        ""),
    RAIN("Rain",        "ambient_rain"),
    FOREST("Forest",    "ambient_forest"),
    OCEAN("Ocean",      "ambient_ocean"),
    WHITE_NOISE("White Noise", "ambient_white_noise"),
    BROWN_NOISE("Brown Noise", "ambient_brown_noise"),
    FIREPLACE("Fireplace",     "ambient_fireplace"),
}

/**
 * Valid interval options in seconds.
 * 15 s, 30 s, 1 min, 2 min, 5 min, 10 min, 15 min, 20 min, 30 min, 45 min, 60 min
 */
enum class IntervalOption(val seconds: Int, val displayName: String) {
    S15(15,   "15 sec"),
    S30(30,   "30 sec"),
    M1(60,    "1 min"),
    M2(120,   "2 min"),
    M5(300,   "5 min"),
    M10(600,  "10 min"),
    M15(900,  "15 min"),
    M20(1200, "20 min"),
    M30(1800, "30 min"),
    M45(2700, "45 min"),
    M60(3600, "60 min"),
}

// ──────────────────────────────────────────────
// Preset
// ──────────────────────────────────────────────

data class Preset(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val durationSec: Int = 20 * 60,
    val warmupSec: Int = 0,
    val cooldownSec: Int = 0,
    val intervalOption: IntervalOption? = null,   // null = no intervals
    val startBell: BellSound = BellSound.TIBETAN_BOWL,
    val intervalBell: BellSound = BellSound.SOFT_CHIME,
    val endBell: BellSound = BellSound.TIBETAN_BOWL,
    val ambientSound: AmbientSound = AmbientSound.NONE,
    val silentMode: Boolean = false,
    val sortOrder: Int = 0,
)

// ──────────────────────────────────────────────
// Session
// ──────────────────────────────────────────────

data class Session(
    val id: UUID = UUID.randomUUID(),
    val startedAt: Instant,
    val endedAt: Instant,
    val plannedDurationSec: Int,
    val actualDurationSec: Int,
    val presetName: String? = null,
    val warmupSec: Int = 0,
    val cooldownSec: Int = 0,
    val intervalSec: Int? = null,
    val bellSound: String,
    val ambientSound: String,
    val silentMode: Boolean,
    val notes: String? = null,
)

// ──────────────────────────────────────────────
// Timer State (emitted by MeditationTimerService)
// ──────────────────────────────────────────────

sealed class TimerState {
    object Idle : TimerState()

    data class Running(
        val phase: TimerPhase,
        val remainingSec: Int,
        val totalSec: Int,
        val elapsedSec: Int,
    ) : TimerState()

    data class Paused(
        val phase: TimerPhase,
        val remainingSec: Int,
        val totalSec: Int,
    ) : TimerState()

    data class Completed(
        val actualDurationSec: Int,
    ) : TimerState()
}

enum class TimerPhase { WARMUP, MEDITATION, COOLDOWN }

// ──────────────────────────────────────────────
// Statistics
// ──────────────────────────────────────────────

data class MeditationStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalSessions: Int,
    val totalMinutes: Int,
    val avgDurationMinutes: Int,
    val weeklySessionCounts: List<Int>,      // Mon–Sun, 7 values
    val dailyGoalMinutes: Int,
    val todayMinutes: Int,
)

// ──────────────────────────────────────────────
// Stress Nudge (from wearable)
// ──────────────────────────────────────────────

data class StressNudge(
    val heartRate: Int,
    val stressLevel: Float,          // 0.0 – 1.0 normalised
    val receivedAt: Instant = Instant.now(),
)
