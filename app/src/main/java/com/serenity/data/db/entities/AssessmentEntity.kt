package com.serenity.data.db.entities

import androidx.room.*
import com.serenity.domain.model.AssessmentParameter
import com.serenity.domain.model.DayAssessment
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * Stores one row per day.
 * answers is serialised as a compact JSON object:
 *   { "MEDITATION_MORNING_5": true, "NO_HARSH_WORDS": false, … }
 * Missing keys = null (unanswered).
 */
@Entity(tableName = "assessments", indices = [Index(value = ["date"], unique = true)])
data class AssessmentEntity(
    @PrimaryKey val id: String,
    val date: String,          // ISO-8601 yyyy-MM-dd
    val answersJson: String,   // JSON object
) {
    fun toDomain(): DayAssessment {
        val json = runCatching { JSONObject(answersJson) }.getOrDefault(JSONObject())
        val answers = AssessmentParameter.entries.associate { param ->
            val key = param.name
            val value: Boolean? = if (json.has(key)) json.getBoolean(key) else null
            param to value
        }
        return DayAssessment(
            id = UUID.fromString(id),
            date = LocalDate.parse(date),
            answers = answers,
        )
    }

    companion object {
        fun fromDomain(d: DayAssessment): AssessmentEntity {
            val json = JSONObject()
            d.answers.forEach { (param, value) ->
                if (value != null) json.put(param.name, value)
            }
            return AssessmentEntity(
                id = d.id.toString(),
                date = d.date.toString(),
                answersJson = json.toString(),
            )
        }
    }
}
