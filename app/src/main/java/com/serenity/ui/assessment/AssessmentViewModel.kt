package com.serenity.ui.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.data.repository.AssessmentRepository
import com.serenity.domain.model.AssessmentCategory
import com.serenity.domain.model.AssessmentParameter
import com.serenity.domain.model.DayAssessment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class AssessmentUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todayAssessment: DayAssessment = DayAssessment(date = LocalDate.now()),
    val monthAssessments: List<DayAssessment> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val viewingMode: ViewingMode = ViewingMode.TODAY,
)

enum class ViewingMode { TODAY, CALENDAR, MONTH_LIST }

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val repo: AssessmentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssessmentUiState())
    val state: StateFlow<AssessmentUiState> = _state.asStateFlow()

    // The "live" assessment for whichever date is selected
    private val _selectedAssessment = MutableStateFlow(DayAssessment(date = LocalDate.now()))
    val selectedAssessment: StateFlow<DayAssessment> = _selectedAssessment.asStateFlow()

    init {
        // Observe today continuously
        viewModelScope.launch {
            repo.observeToday().collect { day ->
                _state.update { it.copy(todayAssessment = day) }
                if (_state.value.selectedDate == LocalDate.now()) {
                    _selectedAssessment.value = day
                }
            }
        }
        loadMonth(YearMonth.now())
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        viewModelScope.launch {
            repo.observeForDate(date).collect { day ->
                _selectedAssessment.value = day
            }
        }
    }

    fun selectMonth(month: YearMonth) {
        _state.update { it.copy(selectedMonth = month) }
        loadMonth(month)
    }

    private fun loadMonth(month: YearMonth) {
        viewModelScope.launch {
            repo.observeMonth(month.year, month.monthValue).collect { list ->
                _state.update { it.copy(monthAssessments = list) }
            }
        }
    }

    fun setViewingMode(mode: ViewingMode) {
        _state.update { it.copy(viewingMode = mode) }
    }

    /** Toggle a parameter's answer: null → true → false → null */
    fun toggleAnswer(param: AssessmentParameter) {
        val current = _selectedAssessment.value
        val existing = current.answers[param]
        val next: Boolean? = when (existing) {
            null  -> true
            true  -> false
            false -> null
        }
        val updated = current.copy(
            answers = current.answers.toMutableMap().also { map ->
                if (next == null) map.remove(param) else map[param] = next
            }
        )
        _selectedAssessment.value = updated
        viewModelScope.launch {
            repo.save(updated)
        }
    }

    /** Directly set yes/no for a parameter */
    fun setAnswer(param: AssessmentParameter, value: Boolean?) {
        val current = _selectedAssessment.value
        val updated = current.copy(
            answers = current.answers.toMutableMap().also { map ->
                if (value == null) map.remove(param) else map[param] = value
            }
        )
        _selectedAssessment.value = updated
        viewModelScope.launch {
            repo.save(updated)
        }
    }

    fun markAllYes() {
        val current = _selectedAssessment.value
        val all = AssessmentParameter.entries.associate { it to true }
        val updated = current.copy(answers = all)
        _selectedAssessment.value = updated
        viewModelScope.launch { repo.save(updated) }
    }

    fun clearDay() {
        val current = _selectedAssessment.value
        val cleared = current.copy(answers = emptyMap())
        _selectedAssessment.value = cleared
        viewModelScope.launch { repo.save(cleared) }
    }

    // Group parameters by category for display
    val parametersByCategory: Map<AssessmentCategory, List<AssessmentParameter>> =
        AssessmentParameter.entries.groupBy { it.category }
}
