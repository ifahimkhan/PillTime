package com.fahim.pilltime.presentation.reminderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.pilltime.data.repository.ReminderRepository
import com.fahim.pilltime.domain.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State for the reminder list screen. */
sealed interface ReminderListUiState {
    data object Loading : ReminderListUiState
    data object Empty : ReminderListUiState
    data class Loaded(
        val reminders: List<Reminder>,
        val exactAlarmPermissionGranted: Boolean,
    ) : ReminderListUiState
}

/**
 * Exposes the reactive reminder list as [StateFlow]. All mutations are delegated to
 * [ReminderRepository] (which keeps the scheduler in sync) — no scheduling logic lives here.
 */
class ReminderListViewModel(
    private val repository: ReminderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReminderListUiState>(ReminderListUiState.Loading)
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeReminders().collect { reminders ->
                _uiState.value = if (reminders.isEmpty()) {
                    ReminderListUiState.Empty
                } else {
                    ReminderListUiState.Loaded(
                        reminders = reminders,
                        exactAlarmPermissionGranted = repository.checkExactAlarmPermission(),
                    )
                }
            }
        }
    }

    fun toggleActive(id: Long) {
        val current = (_uiState.value as? ReminderListUiState.Loaded)
            ?.reminders?.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { repository.setActive(id, !current.isActive) }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch { repository.deleteReminder(id) }
    }
}
