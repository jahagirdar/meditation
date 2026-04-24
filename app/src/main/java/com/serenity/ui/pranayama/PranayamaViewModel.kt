package com.serenity.ui.pranayama

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.data.repository.PranayamaRepository
import com.serenity.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject

// ──────────────────────────────────────────────
// Picker screen state
// ──────────────────────────────────────────────

data class PranayamaPickerState(
    val selectedTechnique: PranayamaTechnique = PranayamaTechnique.BOX_BREATHING,
    val rounds: Int = PranayamaTechnique.BOX_BREATHING.defaultRounds,
    val recentSessions: List<PranayamaSession> = emptyList(),
    val totalPranayamaMinutes: Int = 0,
)

// ──────────────────────────────────────────────
// Sound IDs within SoundPool
// ──────────────────────────────────────────────

private data class PranayamaSounds(
    val inhale: Int    = 0,   // rising soft tone  — prana_inhale.mp3
    val hold: Int      = 0,   // neutral bell      — prana_hold.mp3
    val exhale: Int    = 0,   // descending tone   — prana_exhale.mp3
    val roundEnd: Int  = 0,   // round-complete bell — prana_round_complete.mp3
    val om: Int        = 0,   // Om chant          — prana_om.mp3
    val sessionEnd: Int = 0,  // session complete  — prana_session_end.mp3
)

// ──────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────

@HiltViewModel
class PranayamaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: PranayamaRepository,
) : ViewModel() {

    // ── Picker state ─────────────────────────
    private val _pickerState = MutableStateFlow(PranayamaPickerState())
    val pickerState: StateFlow<PranayamaPickerState> = _pickerState.asStateFlow()

    // ── Session state ─────────────────────────
    private val _sessionState = MutableStateFlow<PranayamaSessionState?>(null)
    val sessionState: StateFlow<PranayamaSessionState?> = _sessionState.asStateFlow()

    private val _sessionComplete = MutableSharedFlow<PranayamaSession>(extraBufferCapacity = 1)
    val sessionComplete: SharedFlow<PranayamaSession> = _sessionComplete.asSharedFlow()

    // Session bookkeeping
    private var sessionJob: Job? = null
    private var sessionStartedAt: Instant = Instant.now()
    private var roundsCompleted = 0

    // Audio
    private var soundPool: SoundPool? = null
    private var sounds = PranayamaSounds()

    // Vibrator
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    init {
        viewModelScope.launch {
            repo.observeAll().collect { sessions ->
                _pickerState.update {
                    it.copy(
                        recentSessions        = sessions.take(5),
                        totalPranayamaMinutes = sessions.sumOf { s -> s.durationSec } / 60,
                    )
                }
            }
        }
        initAudio()
    }

    // ── Picker interactions ───────────────────

    fun selectTechnique(technique: PranayamaTechnique) {
        _pickerState.update { it.copy(selectedTechnique = technique, rounds = technique.defaultRounds) }
    }

    fun setRounds(rounds: Int) {
        _pickerState.update { it.copy(rounds = rounds.coerceIn(1, 30)) }
    }

    // ── Session control ───────────────────────

    fun startSession() {
        val technique = _pickerState.value.selectedTechnique
        val rounds    = _pickerState.value.rounds
        sessionStartedAt = Instant.now()
        roundsCompleted  = 0

        _sessionState.value = PranayamaSessionState(
            technique         = technique,
            currentRound      = 1,
            totalRounds       = rounds,
            currentPhaseIndex = 0,
            phaseRemainingSec = technique.phases[0].durationSec,
        )
        runSession(technique, rounds)
    }

    fun pauseSession() {
        sessionJob?.cancel()
        _sessionState.update { it?.copy(isPaused = true) }
    }

    fun resumeSession() {
        val s = _sessionState.value?.takeIf { it.isPaused } ?: return
        _sessionState.update { it?.copy(isPaused = false) }
        continueFromState(s)
    }

    fun stopSession() {
        sessionJob?.cancel()
        val s = _sessionState.value ?: return
        viewModelScope.launch {
            val endedAt = Instant.now()
            repo.save(PranayamaSession(
                technique       = s.technique,
                roundsCompleted = roundsCompleted,
                totalRounds     = s.totalRounds,
                durationSec     = (endedAt.epochSecond - sessionStartedAt.epochSecond).toInt(),
                startedAt       = sessionStartedAt,
                endedAt         = endedAt,
            ))
        }
        _sessionState.value = null
    }

    // ── Session runner ────────────────────────

    private fun runSession(technique: PranayamaTechnique, totalRounds: Int) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            for (round in 1..totalRounds) {
                technique.phases.forEachIndexed { phaseIndex, phaseSpec ->
                    // Play phase-start cue
                    playCueForPhase(phaseSpec.phase, technique)

                    var remaining = phaseSpec.durationSec
                    while (remaining >= 0) {
                        _sessionState.update {
                            it?.copy(
                                currentRound      = round,
                                currentPhaseIndex = phaseIndex,
                                phaseRemainingSec = remaining,
                            )
                        }
                        if (remaining == 0) break
                        delay(1_000L)
                        remaining--
                    }
                }
                roundsCompleted = round

                // Round-end bell (except after the final round — session end plays instead)
                if (round < totalRounds) {
                    playSound(sounds.roundEnd)
                    haptic(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }

            // Session complete
            playSound(sounds.sessionEnd)
            haptic(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1))

            val endedAt = Instant.now()
            val record  = PranayamaSession(
                technique       = technique,
                roundsCompleted = totalRounds,
                totalRounds     = totalRounds,
                durationSec     = (endedAt.epochSecond - sessionStartedAt.epochSecond).toInt(),
                startedAt       = sessionStartedAt,
                endedAt         = endedAt,
            )
            repo.save(record)
            _sessionState.update { it?.copy(isComplete = true) }
            _sessionComplete.emit(record)
        }
    }

    private fun continueFromState(s: PranayamaSessionState) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            val technique   = s.technique
            val phases      = technique.phases
            val totalRounds = s.totalRounds

            // Resume mid-phase (no new cue — already in this phase)
            var remaining = s.phaseRemainingSec
            while (remaining >= 0) {
                _sessionState.update { it?.copy(phaseRemainingSec = remaining) }
                if (remaining == 0) break
                delay(1_000L)
                remaining--
            }

            // Remaining phases in current round
            for (phaseIndex in (s.currentPhaseIndex + 1) until phases.size) {
                val phaseSpec = phases[phaseIndex]
                playCueForPhase(phaseSpec.phase, technique)
                var rem = phaseSpec.durationSec
                while (rem >= 0) {
                    _sessionState.update { it?.copy(currentPhaseIndex = phaseIndex, phaseRemainingSec = rem) }
                    if (rem == 0) break
                    delay(1_000L)
                    rem--
                }
            }
            roundsCompleted = s.currentRound
            if (s.currentRound < totalRounds) {
                playSound(sounds.roundEnd)
            }

            // Remaining rounds
            for (round in (s.currentRound + 1)..totalRounds) {
                phases.forEachIndexed { phaseIndex, phaseSpec ->
                    playCueForPhase(phaseSpec.phase, technique)
                    var rem = phaseSpec.durationSec
                    while (rem >= 0) {
                        _sessionState.update { it?.copy(currentRound = round, currentPhaseIndex = phaseIndex, phaseRemainingSec = rem) }
                        if (rem == 0) break
                        delay(1_000L)
                        rem--
                    }
                }
                roundsCompleted = round
                if (round < totalRounds) playSound(sounds.roundEnd)
            }

            val endedAt = Instant.now()
            val record  = PranayamaSession(
                technique       = technique,
                roundsCompleted = totalRounds,
                totalRounds     = totalRounds,
                durationSec     = (endedAt.epochSecond - sessionStartedAt.epochSecond).toInt(),
                startedAt       = sessionStartedAt,
                endedAt         = endedAt,
            )
            repo.save(record)
            _sessionState.update { it?.copy(isComplete = true) }
            _sessionComplete.emit(record)
        }
    }

    // ── Phase audio cue ───────────────────────

    /**
     * Maps each breath phase to the correct audio cue.
     *
     * Inhale phases → rising prana_inhale tone
     * Hold phases   → neutral prana_hold bell
     * Exhale phases → descending prana_exhale tone
     * Humming (Bhramari) → Om chant (prana_om) instead of plain exhale
     * Pump (Kapalabhati)  → short inhale tick, no long tone
     * Prepare            → soft hold bell to signal settling
     */
    private fun playCueForPhase(phase: BreathPhase, technique: PranayamaTechnique) {
        val soundId = when (phase) {
            BreathPhase.INHALE,
            BreathPhase.INHALE_L,
            BreathPhase.INHALE_R  -> sounds.inhale

            BreathPhase.HOLD_IN,
            BreathPhase.HOLD_OUT,
            BreathPhase.PREPARE   -> sounds.hold

            BreathPhase.EXHALE,
            BreathPhase.EXHALE_L,
            BreathPhase.EXHALE_R  -> sounds.exhale

            // Bhramari: the humming IS the Om — play Om chant cue
            BreathPhase.HUMMING   -> sounds.om

            // Kapalabhati rapid pump: short tick, low volume
            BreathPhase.PUMP      -> sounds.inhale
        }
        val volume = if (phase == BreathPhase.PUMP) 0.4f else 1.0f
        playSound(soundId, volume)

        // Haptic pattern per phase
        val effect = when (phase) {
            BreathPhase.INHALE,
            BreathPhase.INHALE_L,
            BreathPhase.INHALE_R  -> VibrationEffect.createOneShot(60,  VibrationEffect.DEFAULT_AMPLITUDE)
            BreathPhase.HOLD_IN,
            BreathPhase.HOLD_OUT  -> VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
            BreathPhase.EXHALE,
            BreathPhase.EXHALE_L,
            BreathPhase.EXHALE_R,
            BreathPhase.HUMMING   -> VibrationEffect.createWaveform(longArrayOf(0, 40, 30, 40), -1)
            BreathPhase.PUMP      -> VibrationEffect.createOneShot(20,  VibrationEffect.DEFAULT_AMPLITUDE)
            BreathPhase.PREPARE   -> VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        haptic(effect)
    }

    // ── Audio helpers ─────────────────────────

    private fun playSound(soundId: Int, volume: Float = 1.0f) {
        if (soundId == 0) return      // asset not found — silent fallback
        soundPool?.play(soundId, volume, volume, 1, 0, 1f)
    }

    private fun initAudio() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            ).build()

        fun load(name: String): Int {
            val id = context.resources.getIdentifier(name, "raw", context.packageName)
            return if (id != 0) soundPool!!.load(context, id, 1) else 0
        }

        sounds = PranayamaSounds(
            inhale     = load("prana_inhale"),
            hold       = load("prana_hold"),
            exhale     = load("prana_exhale"),
            roundEnd   = load("prana_round_complete"),
            om         = load("prana_om"),
            sessionEnd = load("prana_session_end"),
        )
    }

    // ── Haptic helper ─────────────────────────

    private fun haptic(effect: VibrationEffect) {
        if (vibrator.hasVibrator()) vibrator.vibrate(effect)
    }

    override fun onCleared() { soundPool?.release(); super.onCleared() }
}
