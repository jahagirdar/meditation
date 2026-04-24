package com.serenity.domain.model

import java.time.Instant
import java.util.UUID

// ──────────────────────────────────────────────
// Breath phase
// ──────────────────────────────────────────────

enum class BreathPhase(
    val label: String,
    val instruction: String,
    val color: Long,          // ARGB packed — used in UI
) {
    INHALE(    "Inhale",     "Breathe in slowly through your nose",          0xFF5B7FA6),
    HOLD_IN(   "Hold",       "Retain the breath gently",                     0xFF4A7C6F),
    EXHALE(    "Exhale",     "Release slowly and completely",                0xFF8C7A60),
    HOLD_OUT(  "Hold",       "Rest before the next inhale",                  0xFF6B5B7B),
    PUMP(      "Pump",       "Sharp forceful exhale through the nose",       0xFFC48B40),
    INHALE_L(  "Inhale (L)", "Close right nostril, breathe in through left", 0xFF5B7FA6),
    INHALE_R(  "Inhale (R)", "Close left nostril, breathe in through right", 0xFF5B7FA6),
    EXHALE_L(  "Exhale (L)", "Close right nostril, breathe out through left",0xFF8C7A60),
    EXHALE_R(  "Exhale (R)", "Close left nostril, breathe out through right",0xFF8C7A60),
    HUMMING(   "Hum",        "Exhale making a steady humming sound",         0xFF3D8080),
    PREPARE(   "Prepare",    "Settle into a comfortable seated posture",     0xFF49454F),
}

// ──────────────────────────────────────────────
// One phase within a technique
// ──────────────────────────────────────────────

data class PhaseSpec(
    val phase: BreathPhase,
    val durationSec: Int,
    /** If true this phase is shown but has no animation (e.g. Kapalabhati rapid pumps) */
    val isRapid: Boolean = false,
    /** Overrides durationSec label for rapid phases (e.g. "20 pumps") */
    val countLabel: String? = null,
)

// ──────────────────────────────────────────────
// Technique definitions
// ──────────────────────────────────────────────

enum class PranayamaTechnique(
    val displayName: String,
    val sanskritName: String,
    val emoji: String,
    val tagline: String,
    val description: String,
    val benefits: List<String>,
    val phases: List<PhaseSpec>,
    val defaultRounds: Int,
    val difficulty: Difficulty,
    val notesForPractitioner: String,
) {
    BOX_BREATHING(
        displayName  = "Box Breathing",
        sanskritName = "Sama Vritti",
        emoji        = "⬜",
        tagline      = "Equal sides, calm mind",
        description  = "Four equal sides of 4 seconds each — inhale, hold, exhale, hold. " +
                       "Used by Navy SEALs and athletes to reduce acute stress.",
        benefits     = listOf("Reduces cortisol", "Improves focus", "Calms the nervous system"),
        phases = listOf(
            PhaseSpec(BreathPhase.INHALE,   4),
            PhaseSpec(BreathPhase.HOLD_IN,  4),
            PhaseSpec(BreathPhase.EXHALE,   4),
            PhaseSpec(BreathPhase.HOLD_OUT, 4),
        ),
        defaultRounds          = 8,
        difficulty             = Difficulty.BEGINNER,
        notesForPractitioner   = "Keep the breath smooth and even. If 4 counts feels short, try 5 or 6.",
    ),

    FOUR_SEVEN_EIGHT(
        displayName  = "4-7-8 Breathing",
        sanskritName = "Vishama Vritti",
        emoji        = "💤",
        tagline      = "Natural tranquiliser for the nervous system",
        description  = "Inhale for 4, hold for 7, exhale for 8. The extended hold and slow " +
                       "exhale activate the parasympathetic nervous system rapidly.",
        benefits     = listOf("Induces sleep", "Reduces anxiety", "Controls cravings"),
        phases = listOf(
            PhaseSpec(BreathPhase.INHALE,   4),
            PhaseSpec(BreathPhase.HOLD_IN,  7),
            PhaseSpec(BreathPhase.EXHALE,   8),
        ),
        defaultRounds          = 4,
        difficulty             = Difficulty.BEGINNER,
        notesForPractitioner   = "Keep the tip of your tongue resting on the ridge behind the upper front teeth throughout.",
    ),

    NADI_SHODHANA(
        displayName  = "Alternate Nostril",
        sanskritName = "Nadi Shodhana",
        emoji        = "🌀",
        tagline      = "Balance ida and pingala nadis",
        description  = "Alternate nostril breathing purifies the subtle energy channels. " +
                       "Use the right hand: thumb closes the right nostril, ring finger closes the left.",
        benefits     = listOf("Balances hemispheres", "Purifies nadis", "Deepens meditation"),
        phases = listOf(
            PhaseSpec(BreathPhase.INHALE_L,  4),
            PhaseSpec(BreathPhase.HOLD_IN,   4),
            PhaseSpec(BreathPhase.EXHALE_R,  4),
            PhaseSpec(BreathPhase.INHALE_R,  4),
            PhaseSpec(BreathPhase.HOLD_IN,   4),
            PhaseSpec(BreathPhase.EXHALE_L,  4),
        ),
        defaultRounds          = 9,
        difficulty             = Difficulty.INTERMEDIATE,
        notesForPractitioner   = "One round = left inhale → right exhale → right inhale → left exhale. " +
                                  "Always end by exhaling through the left nostril.",
    ),

    KAPALABHATI(
        displayName  = "Skull Shining",
        sanskritName = "Kapalabhati",
        emoji        = "✨",
        tagline      = "Cleansing fire breath",
        description  = "Rapid forceful exhalations driven by the lower abdomen, with passive " +
                       "inhalations. One set of 20–30 pumps followed by a retention.",
        benefits     = listOf("Energises body", "Clears sinuses", "Strengthens core"),
        phases = listOf(
            PhaseSpec(BreathPhase.PREPARE,  3),
            PhaseSpec(BreathPhase.PUMP,    20, isRapid = true, countLabel = "20 pumps"),
            PhaseSpec(BreathPhase.INHALE,   2),
            PhaseSpec(BreathPhase.HOLD_IN, 10),
            PhaseSpec(BreathPhase.EXHALE,   4),
        ),
        defaultRounds          = 3,
        difficulty             = Difficulty.INTERMEDIATE,
        notesForPractitioner   = "Not for practice during pregnancy, menstruation, or high blood pressure. " +
                                  "Each pump is a sharp contraction of the abdomen — inhale is passive.",
    ),

    BHRAMARI(
        displayName  = "Humming Bee",
        sanskritName = "Bhramari",
        emoji        = "🐝",
        tagline      = "Inner sound to silence the mind",
        description  = "Inhale fully, then exhale making a steady humming sound like a bee. " +
                       "Plug the ears with thumbs and close eyes for deeper effect.",
        benefits     = listOf("Relieves anxiety", "Reduces blood pressure", "Improves sleep"),
        phases = listOf(
            PhaseSpec(BreathPhase.INHALE,   4),
            PhaseSpec(BreathPhase.HUMMING,  8),
        ),
        defaultRounds          = 7,
        difficulty             = Difficulty.BEGINNER,
        notesForPractitioner   = "Plug both ears gently with your thumbs, close your eyes, and place " +
                                  "fingers lightly on your face. Keep your mouth closed throughout the hum.",
    ),

    UJJAYI(
        displayName  = "Ocean Breath",
        sanskritName = "Ujjayi",
        emoji        = "🌊",
        tagline      = "Victorious breath with gentle constriction",
        description  = "Slightly constrict the back of the throat to create a soft ocean sound " +
                       "on both inhale and exhale. Commonly paired with asana practice.",
        benefits     = listOf("Builds heat", "Enhances focus", "Anchors awareness"),
        phases = listOf(
            PhaseSpec(BreathPhase.INHALE, 5),
            PhaseSpec(BreathPhase.EXHALE, 6),
        ),
        defaultRounds          = 12,
        difficulty             = Difficulty.BEGINNER,
        notesForPractitioner   = "Imagine you are trying to fog up a mirror — that constriction at the " +
                                  "glottis is Ujjayi. Maintain it on both the inhale and exhale.",
    ),

    BHASTRIKA(
        displayName  = "Bellows Breath",
        sanskritName = "Bhastrika",
        emoji        = "🔥",
        tagline      = "Energising forceful complete breath",
        description  = "Both inhalation and exhalation are forced and rapid, like the bellows " +
                       "of a forge. Followed by a deep retention.",
        benefits     = listOf("Increases prana", "Generates heat", "Clears energy blockages"),
        phases = listOf(
            PhaseSpec(BreathPhase.INHALE,   2, isRapid = true),
            PhaseSpec(BreathPhase.EXHALE,   2, isRapid = true),
            PhaseSpec(BreathPhase.INHALE,   4),
            PhaseSpec(BreathPhase.HOLD_IN, 12),
            PhaseSpec(BreathPhase.EXHALE,   6),
        ),
        defaultRounds          = 3,
        difficulty             = Difficulty.ADVANCED,
        notesForPractitioner   = "Start with 10 rapid breath cycles, then do one deep breath with retention. " +
                                  "Avoid if you have heart conditions, epilepsy, or are pregnant.",
    ),
}

enum class Difficulty(val label: String, val color: Long) {
    BEGINNER(    "Beginner",     0xFF4CAF50),
    INTERMEDIATE("Intermediate", 0xFFFF9800),
    ADVANCED(    "Advanced",     0xFFEF5350),
}

// ──────────────────────────────────────────────
// Sapta Vyahritis — seven great utterances
// One is mentally chanted per round, cycling modulo 7
// ──────────────────────────────────────────────

enum class Vyahriti(
    val sanskrit: String,       // Devanagari
    val transliteration: String, // IAST / common romanisation
    val meaning: String,         // English gloss
    val plane: String,           // Cosmological plane
) {
    BHUR(
        sanskrit        = "ॐ भूः",
        transliteration = "Om Bhūr",
        meaning         = "I am the physical plane",
        plane           = "Bhuloka — Earth",
    ),
    BHUVAH(
        sanskrit        = "ॐ भुवः",
        transliteration = "Om Bhuvaḥ",
        meaning         = "I pervade the vital / astral plane",
        plane           = "Bhuvarloka — Atmosphere",
    ),
    SWAHA(
        sanskrit        = "ॐ स्वः",
        transliteration = "Om Svaḥ",
        meaning         = "I radiate the celestial plane",
        plane           = "Svarloka — Heaven",
    ),
    MAHAH(
        sanskrit        = "ॐ महः",
        transliteration = "Om Mahaḥ",
        meaning         = "I abide in the great plane",
        plane           = "Maharloka — The Great Plane",
    ),
    JANAH(
        sanskrit        = "ॐ जनः",
        transliteration = "Om Janaḥ",
        meaning         = "I am in the plane of souls",
        plane           = "Janaloka — Plane of Generation",
    ),
    TAPAH(
        sanskrit        = "ॐ तपः",
        transliteration = "Om Tapaḥ",
        meaning         = "I am the plane of austerity",
        plane           = "Tapoloka — Plane of Austerity",
    ),
    SATYAM(
        sanskrit        = "ॐ सत्यम्",
        transliteration = "Om Satyam",
        meaning         = "I am truth — the supreme plane",
        plane           = "Satyaloka — Plane of Truth",
    );

    companion object {
        /** Return the vyahriti for a given 1-based round number (cycles every 7) */
        fun forRound(round: Int): Vyahriti =
            entries[(round - 1) % entries.size]
    }
}

// ──────────────────────────────────────────────
// Gayatri Mantra — shown when a full cycle of 7 completes
// ──────────────────────────────────────────────

object GayatriMantra {
    const val sanskrit = """ॐ तत्सवितुर्वरेण्यं
भर्गो देवस्य धीमहि
धियो यो नः प्रचोदयात्"""

    const val transliteration = """Om Tat Savitur Vareṇyam
Bhargo Devasya Dhīmahi
Dhiyo Yo Naḥ Prachodayāt"""

    const val meaning = "We meditate on the glory of the Creator who has created the universe, " +
                        "who is worthy of worship, who is the embodiment of knowledge and light, " +
                        "who is the remover of all sins and ignorance. May He enlighten our intellect."
}

// ──────────────────────────────────────────────
// Active session state
// ──────────────────────────────────────────────

data class PranayamaSessionState(
    val technique: PranayamaTechnique,
    val currentRound: Int,
    val totalRounds: Int,
    val currentPhaseIndex: Int,
    val phaseRemainingSec: Int,
    val isPaused: Boolean = false,
    val isComplete: Boolean = false,
) {
    val currentPhase: PhaseSpec
        get() = technique.phases[currentPhaseIndex]

    val roundProgress: Float
        get() = (currentRound - 1).toFloat() / totalRounds.toFloat()

    val phaseProgress: Float
        get() {
            val total = currentPhase.durationSec
            return if (total == 0) 0f
            else 1f - (phaseRemainingSec.toFloat() / total.toFloat())
        }

    /** The vyahriti to be mentally chanted during this round */
    val currentVyahriti: Vyahriti
        get() = Vyahriti.forRound(currentRound)

    /** True when this round completes the 7th vyahriti in a cycle */
    val completesGayatriCycle: Boolean
        get() = currentRound % 7 == 0
}

// ──────────────────────────────────────────────
// Persisted session record
// ──────────────────────────────────────────────

data class PranayamaSession(
    val id: UUID = UUID.randomUUID(),
    val technique: PranayamaTechnique,
    val roundsCompleted: Int,
    val totalRounds: Int,
    val durationSec: Int,
    val startedAt: Instant,
    val endedAt: Instant,
)
