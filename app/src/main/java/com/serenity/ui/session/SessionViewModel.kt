package com.serenity.ui.session

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.data.preferences.UserPreferencesRepository
import com.serenity.data.repository.SessionRepository
import com.serenity.domain.model.*
import com.serenity.service.MeditationTimerService
import com.serenity.service.TimerActions
import com.serenity.service.TimerStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    val timerState: StateFlow<TimerState> = TimerStateHolder.state

    private val _showElapsed = MutableStateFlow(false)
    val showElapsed: StateFlow<Boolean> = _showElapsed.asStateFlow()

    private val _breathingEnabled = MutableStateFlow(true)
    val breathingEnabled: StateFlow<Boolean> = _breathingEnabled.asStateFlow()

    private var currentPreset: Preset? = null

    init {
        viewModelScope.launch {
            prefsRepo.preferences.collect { prefs ->
                _showElapsed.value = prefs.showElapsedTime
                _breathingEnabled.value = prefs.breathingAnimation
            }
        }
    }

    fun startSession(preset: Preset, context: Context) {
        currentPreset = preset
        // Register preset with service via a static holder
        ActivePresetHolder.preset = preset
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = TimerActions.START
        }
        context.startForegroundService(intent)
    }

    fun pauseSession(context: Context) {
        context.startService(
            Intent(context, MeditationTimerService::class.java).apply {
                action = TimerActions.PAUSE
            }
        )
    }

    fun resumeSession(context: Context) {
        context.startService(
            Intent(context, MeditationTimerService::class.java).apply {
                action = TimerActions.RESUME
            }
        )
    }

    fun stopSession(context: Context) {
        context.startService(
            Intent(context, MeditationTimerService::class.java).apply {
                action = TimerActions.STOP
            }
        )
    }

    fun saveNotes(sessionId: java.util.UUID, notes: String) {
        viewModelScope.launch {
            sessionRepo.updateNotes(sessionId, notes)
        }
    }
}

/** Simple singleton to pass preset to service without serialization */
object ActivePresetHolder {
    var preset: Preset? = null
}
