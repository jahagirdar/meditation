package com.serenity.data.repository

import com.serenity.data.db.DailyBucket
import com.serenity.data.db.PresetDao
import com.serenity.data.db.SessionDao
import com.serenity.data.db.entities.PresetEntity
import com.serenity.data.db.entities.SessionEntity
import com.serenity.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ──────────────────────────────────────────────
// Session Repository
// ──────────────────────────────────────────────

@Singleton
class SessionRepository @Inject constructor(
    private val dao: SessionDao,
) {
    fun observeAll(): Flow<List<Session>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeSince(from: Instant): Flow<List<Session>> =
        dao.observeSince(from.toEpochMilli()).map { list -> list.map { it.toDomain() } }

    suspend fun save(session: Session) = dao.insert(SessionEntity.fromDomain(session))

    suspend fun updateNotes(id: UUID, notes: String) = dao.updateNotes(id.toString(), notes)

    suspend fun delete(session: Session) = dao.delete(SessionEntity.fromDomain(session))

    suspend fun computeStats(dailyGoalMinutes: Int): MeditationStats {
        val totalSec = dao.totalSeconds()

        // Buckets for last 90 days
        val ninetyDaysAgo = Instant.now().minusSeconds(90L * 86400)
        val buckets: List<DailyBucket> = dao.dailyBuckets(ninetyDaysAgo.toEpochMilli())

        // Current and longest streak (goal-aware)
        val goalSec = dailyGoalMinutes * 60
        val today = LocalDate.now(ZoneId.systemDefault())
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        val bucketMap = buckets.associateBy { it.dayBucket }

        // Walk backwards from today
        var streak = 0
        var dayOffset = 0L
        while (true) {
            val day = today.minusDays(dayOffset)
            val key = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 86400000L
            val daySec = bucketMap[key]?.secs ?: 0
            if (daySec >= goalSec) {
                streak++
                if (dayOffset == 0L || dayOffset == 1L) currentStreak = streak
            } else {
                if (dayOffset > 1L) break
            }
            dayOffset++
            if (dayOffset > 90) break
        }

        // Longest streak from full history
        var running = 0
        var prevKey = -1L
        buckets.sortedBy { it.dayBucket }.forEach { b ->
            if (b.secs >= goalSec) {
                running = if (prevKey >= 0 && b.dayBucket == prevKey + 1) running + 1 else 1
                if (running > longestStreak) longestStreak = running
            } else {
                running = 0
            }
            prevKey = b.dayBucket
        }

        // Weekly counts (Mon–Sun this week)
        val weekStart = today.with(java.time.DayOfWeek.MONDAY)
        val weeklyCounts = (0..6).map { offset ->
            val day = weekStart.plusDays(offset.toLong())
            val key = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 86400000L
            if (bucketMap[key]?.secs != null) 1 else 0
        }

        val todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val todaySec = dao.secondsToday(todayStart.toEpochMilli())

        val allSessions = dao.observeAll()
        val sessionCount = buckets.sumOf { if (it.secs > 0) 1 else 0 }
        val avgMin = if (sessionCount > 0) (totalSec / sessionCount) / 60 else 0

        return MeditationStats(
            currentStreak       = currentStreak,
            longestStreak       = longestStreak,
            totalSessions       = buckets.size,
            totalMinutes        = totalSec / 60,
            avgDurationMinutes  = avgMin,
            weeklySessionCounts = weeklyCounts,
            dailyGoalMinutes    = dailyGoalMinutes,
            todayMinutes        = todaySec / 60,
        )
    }
}

// ──────────────────────────────────────────────
// Preset Repository
// ──────────────────────────────────────────────

@Singleton
class PresetRepository @Inject constructor(
    private val dao: PresetDao,
) {
    fun observeAll(): Flow<List<Preset>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun save(preset: Preset) = dao.upsert(PresetEntity.fromDomain(preset))

    suspend fun delete(preset: Preset) = dao.delete(PresetEntity.fromDomain(preset))

    /** Seed default presets if DB is empty */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() == 0) {
            listOf(
                Preset(name = "Quick 5", durationSec = 5 * 60, sortOrder = 0),
                Preset(name = "Morning 20", durationSec = 20 * 60, warmupSec = 60, sortOrder = 1),
                Preset(name = "Deep 45",
                    durationSec = 45 * 60,
                    warmupSec = 60,
                    cooldownSec = 120,
                    intervalOption = IntervalOption.M15,
                    sortOrder = 2
                ),
            ).forEach { save(it) }
        }
    }
}
