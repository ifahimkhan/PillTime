package com.fahim.pilltime.presentation.reminderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.pilltime.core.platform.hasNotificationPermission
import com.fahim.pilltime.data.repository.ReminderRepository
import com.fahim.pilltime.domain.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State for the reminder list screen. */
sealed interface ReminderListUiState {
    data object Loading : ReminderListUiState
    data class Empty(
        val exactAlarmPermissionGranted: Boolean,
        val notificationPermissionGranted: Boolean,
    ) : ReminderListUiState
    data class Loaded(
        val reminders: List<Reminder>,
        val exactAlarmPermissionGranted: Boolean,
        val notificationPermissionGranted: Boolean,
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

    // Latest reminders, retained so permission re-checks (on app resume) can rebuild the
    // Loaded state without waiting for the DB flow to re-emit.
    private var lastReminders: List<Reminder> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeReminders().collect { reminders ->
                lastReminders = reminders
                _uiState.value = buildState(reminders)
            }
        }
    }

    /** Re-evaluate permission state (e.g. after returning from system settings) and refresh. */
    fun refreshPermissions() {
        viewModelScope.launch { _uiState.value = buildState(lastReminders) }
    }

    private suspend fun buildState(reminders: List<Reminder>): ReminderListUiState {
        val exactAlarmGranted = repository.checkExactAlarmPermission()
        val notificationGranted = hasNotificationPermission()
        return if (reminders.isEmpty()) {
            ReminderListUiState.Empty(exactAlarmGranted, notificationGranted)
        } else {
            ReminderListUiState.Loaded(reminders, exactAlarmGranted, notificationGranted)
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
