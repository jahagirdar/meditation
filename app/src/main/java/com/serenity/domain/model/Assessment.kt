package com.serenity.domain.model

import java.time.LocalDate
import java.util.UUID

// ──────────────────────────────────────────────
// The 20 Dhamma self-assessment parameters
// ──────────────────────────────────────────────

enum class AssessmentParameter(
    val number: Int,
    val title: String,
    val description: String,
    val category: AssessmentCategory,
) {
    MEDITATION_MORNING_5(
        1, "Morning 5-min sit",
        "5 minutes meditation after waking up",
        AssessmentCategory.PRACTICE,
    ),
    MEDITATION_MORNING_1H(
        2, "Morning 1-hour meditation",
        "Full 1-hour morning meditation session",
        AssessmentCategory.PRACTICE,
    ),
    NO_HARSH_WORDS(
        3, "No harsh or rude words",
        "Able to abstain from speaking harsh or rude words",
        AssessmentCategory.SPEECH,
    ),
    NO_SLANDER(
        4, "No slander",
        "Able to abstain from slander",
        AssessmentCategory.SPEECH,
    ),
    NO_LYING(
        5, "No lying",
        "Able to abstain from lying",
        AssessmentCategory.SPEECH,
    ),
    NO_IDLE_CHATTER(
        6, "No gossip or idle chatter",
        "Able to abstain from gossip and idle chitchatting",
        AssessmentCategory.SPEECH,
    ),
    EQUANIMITY_FEAR(
        7, "Equanimity with fear or anxiety",
        "Observed breath/sensations and maintained equanimity (with understanding of anicca) when fear or anxiety arose",
        AssessmentCategory.MIND,
    ),
    NO_STEALING(
        8, "No taking what is not given",
        "Able to abstain from taking what is not given",
        AssessmentCategory.CONDUCT,
    ),
    NO_SEXUAL_MISCONDUCT(
        9, "No sexual misconduct",
        "Able to abstain from sexual misconduct",
        AssessmentCategory.CONDUCT,
    ),
    TOLERATING_CRITICISM(
        10, "Tolerated criticism",
        "Ability to tolerate criticism",
        AssessmentCategory.MIND,
    ),
    EQUANIMITY_PLEASANT(
        11, "Equanimity in pleasant situations",
        "Able to maintain equanimity in the face of PLEASANT situations and abide in anicca",
        AssessmentCategory.MIND,
    ),
    EQUANIMITY_UNPLEASANT(
        12, "Equanimity in unpleasant situations",
        "Able to maintain equanimity in the face of UNPLEASANT situations and abide in anicca",
        AssessmentCategory.MIND,
    ),
    BREATH_DURING_FREE_TIME(
        13, "Observing breath in free time",
        "Observing breath/sensations during free time",
        AssessmentCategory.PRACTICE,
    ),
    METTA_TO_FAMILY(
        14, "Metta towards family",
        "Able to forgive family members for their mistakes and maintain metta towards them",
        AssessmentCategory.RELATIONS,
    ),
    ADMIT_MISTAKES(
        15, "Admit mistakes and seek forgiveness",
        "When committing a mistake, able to admit it and seek forgiveness",
        AssessmentCategory.CONDUCT,
    ),
    METTA_BHAVANA(
        16, "Metta bhavana in daily life",
        "Able to make use of metta bhavana in various situations of daily life",
        AssessmentCategory.PRACTICE,
    ),
    HARMONIOUS_AT_WORK(
        17, "Harmonious with colleagues",
        "Behaviour with colleagues in the job is harmonious",
        AssessmentCategory.RELATIONS,
    ),
    RIGHTEOUS_DUTY(
        18, "Righteous duty",
        "Righteously doing duty",
        AssessmentCategory.CONDUCT,
    ),
    MEDITATION_EVENING_1H(
        19, "Evening 1-hour meditation",
        "Full 1-hour evening meditation session",
        AssessmentCategory.PRACTICE,
    ),
    MEDITATION_EVENING_5(
        20, "Evening 5-min sit",
        "5 minutes meditation before sleep",
        AssessmentCategory.PRACTICE,
    ),
}

enum class AssessmentCategory(val displayName: String, val emoji: String) {
    PRACTICE("Practice",   "🧘"),
    SPEECH("Speech",       "🗣️"),
    MIND("Mind",           "🌊"),
    CONDUCT("Conduct",     "⚖️"),
    RELATIONS("Relations", "🤝"),
}

// ──────────────────────────────────────────────
// One day's assessment record
// ──────────────────────────────────────────────

/**
 * A single day's assessment. Each parameter can be:
 *  - null  → not yet answered (blank, today or future)
 *  - true  → yes ✓
 *  - false → no  ✗
 */
data class DayAssessment(
    val id: UUID = UUID.randomUUID(),
    val date: LocalDate,
    val answers: Map<AssessmentParameter, Boolean?> = emptyMap(),
) {
    /** Score as a fraction 0.0–1.0 (only counting answered parameters) */
    fun score(): Float {
        val answered = answers.values.filterNotNull()
        if (answered.isEmpty()) return 0f
        return answered.count { it }.toFloat() / answered.size.toFloat()
    }

    /** True if every parameter has been answered */
    fun isComplete(): Boolean = answers.size == AssessmentParameter.entries.size &&
        answers.values.none { it == null }

    fun answeredCount(): Int = answers.values.count { it != null }
}
