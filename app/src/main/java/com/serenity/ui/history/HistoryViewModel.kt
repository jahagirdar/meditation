package com.serenity.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.data.preferences.UserPreferencesRepository
import com.serenity.data.repository.SessionRepository
import com.serenity.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<Session> = emptyList(),
    val stats: MeditationStats? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sessionRepo.observeAll(),
                prefsRepo.preferences,
            ) { sessions, prefs -> sessions to prefs }.collect { (sessions, prefs) ->
                val stats = sessionRepo.computeStats(prefs.dailyGoalMinutes)
                _state.update { it.copy(sessions = sessions, stats = stats) }
            }
        }
    }
}
