package com.serenity.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.serenity.MainActivity
import com.serenity.R
import com.serenity.data.repository.SessionRepository
import com.serenity.domain.model.*
import com.serenity.ui.session.ActivePresetHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

object TimerActions {
    const val START  = "com.serenity.timer.START"
    const val PAUSE  = "com.serenity.timer.PAUSE"
    const val RESUME = "com.serenity.timer.RESUME"
    const val STOP   = "com.serenity.timer.STOP"
}

object TimerStateHolder {
    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state

    private val _stressNudge = MutableSharedFlow<StressNudge>(extraBufferCapacity = 1)
    val stressNudge: SharedFlow<StressNudge> = _stressNudge

    fun emit(s: TimerState)       { _state.value = s }
    fun emitNudge(n: StressNudge) { _stressNudge.tryEmit(n) }
}

@AndroidEntryPoint
class MeditationTimerService : Service() {

    @Inject lateinit var sessionRepository: SessionRepository

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var soundPool: SoundPool? = null
    private var mediaPlayer: MediaPlayer? = null
    private val soundIds = mutableMapOf<BellSound, Int>()
    private var currentPreset: Preset? = null
    private var sessionStartedAt: Instant = Instant.now()
    private var sessionId: UUID = UUID.randomUUID()

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL = "meditation_timer"
        const val NOTIFICATION_ID      = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initSoundPool()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            TimerActions.START -> {
                val preset = ActivePresetHolder.preset
                if (preset != null) {
                    currentPreset    = preset
                    sessionId        = UUID.randomUUID()
                    sessionStartedAt = Instant.now()
                    startTimer(preset)
                } else stopSelf()
            }
            TimerActions.PAUSE  -> pauseTimer()
            TimerActions.RESUME -> resumeTimer()
            TimerActions.STOP   -> stopTimer(earlyStop = true)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() { scope.cancel(); soundPool?.release(); mediaPlayer?.release(); super.onDestroy() }

    // ── Phases ──

    private data class Phase(val timerPhase: TimerPhase, val durationSec: Int)

    private fun buildPhases(p: Preset) = buildList<Phase> {
        if (p.warmupSec > 0)   add(Phase(TimerPhase.WARMUP,     p.warmupSec))
        add(Phase(TimerPhase.MEDITATION, p.durationSec))
        if (p.cooldownSec > 0) add(Phase(TimerPhase.COOLDOWN,   p.cooldownSec))
    }

    // ── Timer loop ──

    private fun startTimer(preset: Preset) {
        timerJob?.cancel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
        if (preset.ambientSound != AmbientSound.NONE && !preset.silentMode) startAmbient(preset.ambientSound)

        timerJob = scope.launch {
            val phases = buildPhases(preset)
            var globalElapsed = 0

            for (phase in phases) {
                when (phase.timerPhase) {
                    TimerPhase.WARMUP     -> Unit
                    TimerPhase.MEDITATION -> playBell(preset.startBell,   preset.silentMode, VibPattern.START)
                    TimerPhase.COOLDOWN   -> playBell(preset.intervalBell, preset.silentMode, VibPattern.INTERVAL)
                }
                var remaining = phase.durationSec
                while (remaining > 0) {
                    TimerStateHolder.emit(TimerState.Running(phase.timerPhase, remaining, phase.durationSec, globalElapsed))
                    updateNotification(remaining)
                    delay(1_000L)
                    remaining--; globalElapsed++
                    if (phase.timerPhase == TimerPhase.MEDITATION) {
                        val intervalSec = preset.intervalOption?.seconds
                        val elapsed = phase.durationSec - remaining
                        if (intervalSec != null && elapsed > 0 && elapsed % intervalSec == 0 && remaining > 0)
                            playBell(preset.intervalBell, preset.silentMode, VibPattern.INTERVAL)
                    }
                }
            }

            playBell(preset.endBell, preset.silentMode, VibPattern.END)
            fadeOutAmbient()
            val endedAt = Instant.now()
            sessionRepository.save(Session(
                id = sessionId, startedAt = sessionStartedAt, endedAt = endedAt,
                plannedDurationSec = preset.durationSec,
                actualDurationSec  = (endedAt.epochSecond - sessionStartedAt.epochSecond).toInt(),
                presetName = preset.name.ifBlank { null },
                warmupSec = preset.warmupSec, cooldownSec = preset.cooldownSec,
                intervalSec = preset.intervalOption?.seconds,
                bellSound = preset.startBell.name, ambientSound = preset.ambientSound.name,
                silentMode = preset.silentMode,
            ))
            TimerStateHolder.emit(TimerState.Completed((endedAt.epochSecond - sessionStartedAt.epochSecond).toInt()))
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel(); mediaPlayer?.pause()
        val s = TimerStateHolder.state.value
        if (s is TimerState.Running) TimerStateHolder.emit(TimerState.Paused(s.phase, s.remainingSec, s.totalSec))
        updateNotificationText("Paused")
    }

    private fun resumeTimer() {
        val paused = TimerStateHolder.state.value as? TimerState.Paused ?: return
        val preset = currentPreset ?: return
        mediaPlayer?.start(); timerJob?.cancel()
        timerJob = scope.launch {
            var remaining = paused.remainingSec
            while (remaining > 0) {
                TimerStateHolder.emit(TimerState.Running(paused.phase, remaining, paused.totalSec, paused.totalSec - remaining))
                updateNotification(remaining)
                delay(1_000L); remaining--
                if (paused.phase == TimerPhase.MEDITATION) {
                    val intervalSec = preset.intervalOption?.seconds
                    val elapsed = paused.totalSec - remaining
                    if (intervalSec != null && elapsed > 0 && elapsed % intervalSec == 0 && remaining > 0)
                        playBell(preset.intervalBell, preset.silentMode, VibPattern.INTERVAL)
                }
            }
            playBell(preset.endBell, preset.silentMode, VibPattern.END); fadeOutAmbient()
            TimerStateHolder.emit(TimerState.Completed(paused.totalSec - paused.remainingSec))
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
        }
    }

    private fun stopTimer(earlyStop: Boolean) {
        timerJob?.cancel(); fadeOutAmbient()
        if (earlyStop) {
            val elapsed = when (val s = TimerStateHolder.state.value) {
                is TimerState.Running -> s.elapsedSec
                is TimerState.Paused  -> s.totalSec - s.remainingSec
                else -> 0
            }
            scope.launch {
                currentPreset?.let { p ->
                    sessionRepository.save(Session(
                        id = sessionId, startedAt = sessionStartedAt, endedAt = Instant.now(),
                        plannedDurationSec = p.durationSec, actualDurationSec = elapsed,
                        presetName = p.name.ifBlank { null }, warmupSec = p.warmupSec, cooldownSec = p.cooldownSec,
                        intervalSec = p.intervalOption?.seconds, bellSound = p.startBell.name,
                        ambientSound = p.ambientSound.name, silentMode = p.silentMode,
                    ))
                }
            }
        }
        TimerStateHolder.emit(TimerState.Idle); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    // ── Audio ──

    private fun initSoundPool() {
        soundPool = SoundPool.Builder().setMaxStreams(3)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).build()
        BellSound.entries.filter { it != BellSound.SILENT }.forEach { bell ->
            val id = resources.getIdentifier(bell.rawRes, "raw", packageName)
            if (id != 0) soundIds[bell] = soundPool!!.load(this, id, 1)
        }
    }

    private fun playBell(bell: BellSound, silent: Boolean, pattern: VibPattern) {
        if (silent || bell == BellSound.SILENT) { vibrate(pattern); return }
        soundIds[bell]?.let { soundPool?.play(it, 1f, 1f, 1, 0, 1f) }
    }

    private fun startAmbient(sound: AmbientSound) {
        val id = resources.getIdentifier(sound.rawRes, "raw", packageName)
        if (id == 0) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, id)?.apply {
            isLooping = true; setVolume(0f, 0f); start()
            scope.launch { repeat(30) { i -> setVolume(i / 30f, i / 30f); delay(100) } }
        }
    }

    private fun fadeOutAmbient() {
        val mp = mediaPlayer ?: return
        scope.launch {
            repeat(30) { i -> mp.setVolume(1f - i / 30f, 1f - i / 30f); delay(100) }
            mp.stop(); mp.release(); mediaPlayer = null
        }
    }

    // ── Haptics ──

    private enum class VibPattern { START, INTERVAL, END }

    private fun vibrate(pattern: VibPattern) {
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(when (pattern) {
            VibPattern.START    -> VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80, 60, 80), -1)
            VibPattern.INTERVAL -> VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
            VibPattern.END      -> VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1)
        })
    }

    // ── Notification ──

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(NOTIFICATION_CHANNEL, "Meditation Timer",
                NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null); enableVibration(false) }
        )
    }

    private fun buildNotification(text: String): Notification {
        val openPi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getService(this, 1,
            Intent(this, MeditationTimerService::class.java).apply { action = TimerActions.STOP },
            PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_meditation)
            .setContentTitle("Meditating").setContentText(text)
            .setContentIntent(openPi).addAction(R.drawable.ic_stop, "End", stopPi)
            .setOngoing(true).setSilent(true).build()
    }

    private fun updateNotification(sec: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification("%d:%02d remaining".format(sec / 60, sec % 60)))
    }

    private fun updateNotificationText(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }
}
