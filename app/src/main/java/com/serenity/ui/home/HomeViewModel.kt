package com.serenity.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.data.preferences.UserPreferencesRepository
import com.serenity.data.repository.AssessmentRepository
import com.serenity.data.repository.PresetRepository
import com.serenity.data.repository.SessionRepository
import com.serenity.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val activePreset: Preset = Preset(name = "Quick Start"),
    val presets: List<Preset> = emptyList(),
    val currentStreak: Int = 0,
    val todayMinutes: Int = 0,
    val dailyGoalMinutes: Int = 10,
    val todayAssessmentAnswered: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val presetRepo: PresetRepository,
    private val sessionRepo: SessionRepository,
    private val assessmentRepo: AssessmentRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { presetRepo.seedDefaultsIfEmpty() }

        viewModelScope.launch {
            combine(
                presetRepo.observeAll(),
                prefsRepo.preferences,
                assessmentRepo.observeToday(),
            ) { presets, prefs, todayAssessment ->
                Triple(presets, prefs, todayAssessment)
            }.collect { (presets, prefs, todayAssessment) ->
                val stats = sessionRepo.computeStats(prefs.dailyGoalMinutes)
                val activePreset = prefs.lastPresetId?.let { id ->
                    presets.firstOrNull { it.id.toString() == id }
                } ?: presets.firstOrNull() ?: _state.value.activePreset

                _state.update {
                    it.copy(
                        presets = presets,
                        activePreset = activePreset,
                        currentStreak = stats.currentStreak,
                        todayMinutes = stats.todayMinutes,
                        dailyGoalMinutes = prefs.dailyGoalMinutes,
                        todayAssessmentAnswered = todayAssessment.answeredCount(),
                    )
                }
            }
        }
    }

    fun selectPreset(preset: Preset) {
        _state.update { it.copy(activePreset = preset) }
        viewModelScope.launch {
            prefsRepo.update { it.copy(lastPresetId = preset.id.toString()) }
        }
    }

    fun setDuration(seconds: Int) {
        val updated = _state.value.activePreset.copy(durationSec = seconds)
        _state.update { it.copy(activePreset = updated) }
    }

    fun updatePreset(preset: Preset) {
        _state.update { it.copy(activePreset = preset) }
        viewModelScope.launch {
            presetRepo.save(preset)
            prefsRepo.update { it.copy(lastPresetId = preset.id.toString()) }
        }
    }

    fun saveNewPreset(preset: Preset) {
        val newPreset = preset.copy(id = UUID.randomUUID())
        viewModelScope.launch { presetRepo.save(newPreset) }
        _state.update { it.copy(activePreset = newPreset) }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch { presetRepo.delete(preset) }
    }
}
