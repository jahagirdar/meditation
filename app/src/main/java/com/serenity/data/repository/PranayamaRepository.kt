package com.serenity.data.repository

import com.serenity.data.db.PranayamaDao
import com.serenity.data.db.entities.PranayamaSessionEntity
import com.serenity.domain.model.PranayamaSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PranayamaRepository @Inject constructor(
    private val dao: PranayamaDao,
) {
    fun observeAll(): Flow<List<PranayamaSession>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    fun observeSince(from: Instant): Flow<List<PranayamaSession>> =
        dao.observeSince(from.toEpochMilli()).map { it.map { e -> e.toDomain() } }

    suspend fun save(session: PranayamaSession) =
        dao.insert(PranayamaSessionEntity.fromDomain(session))

    suspend fun delete(session: PranayamaSession) =
        dao.delete(PranayamaSessionEntity.fromDomain(session))

    suspend fun totalSessions(): Int = dao.count()
    suspend fun totalMinutes(): Int  = dao.totalSeconds() / 60
}
