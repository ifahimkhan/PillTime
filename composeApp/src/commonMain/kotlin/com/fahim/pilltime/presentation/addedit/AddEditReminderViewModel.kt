package com.fahim.pilltime.presentation.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.pilltime.data.repository.ReminderRepository
import com.fahim.pilltime.domain.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Form state for adding/editing a reminder. [canSave] gates the save button; [saved] drives nav. */
data class AddEditReminderUiState(
    val medicineName: String = "",
    val dosage: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val isEditing: Boolean = false,
    // False only while an edit's existing values are still loading; lets the UI build the time
    // picker once with the correct initial values (TimePickerState has no public setter).
    val isInitialized: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = medicineName.isNotBlank()
}

/**
 * Backs the add/edit screen. When [reminderId] is non-null the form is prefilled and [save] updates
 * the existing reminder; otherwise [save] inserts a new one. All persistence + (re)scheduling is
 * handled by [ReminderRepository].
 */
class AddEditReminderViewModel(
    private val repository: ReminderRepository,
    private val reminderId: Long? = null,
) : ViewModel() {

    // Add mode is ready immediately; edit mode waits for the existing reminder to load.
    private val _uiState = MutableStateFlow(
        AddEditReminderUiState(isEditing = reminderId != null, isInitialized = reminderId == null),
    )
    val uiState: StateFlow<AddEditReminderUiState> = _uiState.asStateFlow()

    // Preserved across an edit so saving never silently re-activates a paused reminder.
    private var editingIsActive: Boolean = true

    init {
        if (reminderId != null) {
            viewModelScope.launch {
                val r = repository.getReminder(reminderId)
                if (r != null) {
                    editingIsActive = r.isActive
                    _uiState.update {
                        it.copy(
                            medicineName = r.medicineName,
                            dosage = r.dosage.orEmpty(),
                            hour = r.hour,
                            minute = r.minute,
                            isEditing = true,
                            isInitialized = true,
                        )
                    }
                } else {
                    // Reminder vanished (e.g. deleted elsewhere) — treat as add so the UI still works.
                    _uiState.update { it.copy(isInitialized = true) }
                }
            }
        }
    }

    fun onMedicineNameChange(value: String) = _uiState.update { it.copy(medicineName = value) }
    fun onDosageChange(value: String) = _uiState.update { it.copy(dosage = value) }
    fun onTimeChange(hour: Int, minute: Int) = _uiState.update { it.copy(hour = hour, minute = minute) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            if (reminderId == null) {
                repository.addReminder(
                    medicineName = state.medicineName.trim(),
                    dosage = state.dosage.trim().ifBlank { null },
                    hour = state.hour,
                    minute = state.minute,
                )
            } else {
                repository.updateReminder(
                    Reminder(
                        id = reminderId,
                        medicineName = state.medicineName.trim(),
                        dosage = state.dosage.trim().ifBlank { null },
                        hour = state.hour,
                        minute = state.minute,
                        isActive = editingIsActive,
                    ),
                )
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
