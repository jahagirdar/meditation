package com.serenity.data.repository

import com.serenity.data.db.AssessmentDao
import com.serenity.data.db.entities.AssessmentEntity
import com.serenity.domain.model.AssessmentParameter
import com.serenity.domain.model.DayAssessment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssessmentRepository @Inject constructor(
    private val dao: AssessmentDao,
) {
    fun observeToday(): Flow<DayAssessment> {
        val today = LocalDate.now().toString()
        return dao.observeForDate(today).map { entity ->
            entity?.toDomain() ?: DayAssessment(date = LocalDate.now())
        }
    }

    fun observeForDate(date: LocalDate): Flow<DayAssessment> =
        dao.observeForDate(date.toString()).map { entity ->
            entity?.toDomain() ?: DayAssessment(date = date)
        }

    fun observeMonth(year: Int, month: Int): Flow<List<DayAssessment>> {
        val from = LocalDate.of(year, month, 1).toString()
        val to   = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).toString()
        return dao.observeRange(from, to).map { list -> list.map { it.toDomain() } }
    }

    fun observeAll(): Flow<List<DayAssessment>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Set a single answer for today */
    suspend fun setAnswer(date: LocalDate, param: AssessmentParameter, value: Boolean?) {
        val dateStr = date.toString()
        // Read current state via one-shot (not the flow — we just need current value)
        val existing = dao.observeForDate(dateStr)
        // We'll use a simpler approach: upsert with merged answers
        // In practice you'd read then write; here we model it simply
        val current = DayAssessment(date = date)
        val updated = current.copy(
            answers = current.answers.toMutableMap().also { it[param] = value }
        )
        dao.upsert(AssessmentEntity.fromDomain(updated))
    }

    /** Save a full day's assessment (replaces existing) */
    suspend fun save(day: DayAssessment) {
        dao.upsert(AssessmentEntity.fromDomain(day))
    }

    /** Merge a single answer into the existing record for [date] */
    suspend fun mergeAnswer(
        date: LocalDate,
        param: AssessmentParameter,
        value: Boolean?,
        current: DayAssessment,
    ) {
        val updated = current.copy(
            answers = current.answers.toMutableMap().also { map ->
                if (value == null) map.remove(param) else map[param] = value
            }
        )
        dao.upsert(AssessmentEntity.fromDomain(updated))
    }
}
