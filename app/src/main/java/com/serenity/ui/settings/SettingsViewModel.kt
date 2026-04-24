package com.serenity.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.data.preferences.AppPreferences
import com.serenity.data.preferences.UserPreferencesRepository
import com.serenity.service.ReminderReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: UserPreferencesRepository,
) : ViewModel() {

    val prefs: StateFlow<AppPreferences> = repo.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppPreferences(),
    )

    fun setTheme(mode: String) = update { it.copy(themeMode = mode) }
    fun setAccent(key: String) = update { it.copy(accentColour = key) }
    fun setShowElapsed(v: Boolean) = update { it.copy(showElapsedTime = v) }
    fun setBreathingAnim(v: Boolean) = update { it.copy(breathingAnimation = v) }
    fun setDailyGoal(v: Int) = update { it.copy(dailyGoalMinutes = v) }
    fun setDimScreen(v: Int) = update { it.copy(dimScreenAfterSec = v) }
    fun setStressNudge(v: Boolean) = update { it.copy(stressNudgeEnabled = v) }

    fun setReminder(index: Int, time: String) {
        update { p ->
            when (index) {
                0 -> p.copy(reminder1Time = time)
                1 -> p.copy(reminder2Time = time)
                else -> p.copy(reminder3Time = time)
            }
        }
        scheduleReminder(index, time)
    }

    fun clearReminder(index: Int) {
        update { p ->
            when (index) {
                0 -> p.copy(reminder1Time = null)
                1 -> p.copy(reminder2Time = null)
                else -> p.copy(reminder3Time = null)
            }
        }
        ReminderReceiver.cancel(context, index)
    }

    private fun scheduleReminder(index: Int, time: String) {
        val parts = time.split(":").map { it.toInt() }
        if (parts.size == 2) {
            ReminderReceiver.schedule(context, index, parts[0], parts[1])
        }
    }

    private fun update(transform: suspend (AppPreferences) -> AppPreferences) {
        viewModelScope.launch { repo.update(transform) }
    }
}
