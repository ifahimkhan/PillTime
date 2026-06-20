# Prompt 4 — State / Data Layer (Repository + ViewModels)

> **Before starting**: Read `CLAUDE.md` for full project context. Do not proceed without it.

---

## Role

You are a senior Kotlin Multiplatform engineer building the repository and ViewModel layer that connects the local database to the UI.

---

## Goal

`ReminderRepository` exposes a reactive list of reminders backed by SQLDelight and keeps the `ReminderScheduler` in sync on every change. `ReminderListViewModel` and `AddEditReminderViewModel` expose `StateFlow<UiState>` ready for the UI screens in prompt 5.

---

## Context Snapshot

- **Stack**: SQLDelight queries (prompt 2), `ReminderScheduler` (prompt 3), Koin for DI, `StateFlow` for state
- **Relevant existing files**:
  - `sqldelight/com/pilltime/db/Reminder.sq` — generates `Reminder` rows + queries
  - `core/notification/ReminderScheduler.kt` — expect interface to schedule/cancel alarms
- **Upstream dependencies**: prompts 2 and 3 must be complete
- **Downstream consumers**: `prompt_5_ui.md` — screens collect these ViewModels' state

---

## Task Breakdown

1. **Inspect first**
   - Read the generated SQLDelight query signatures and the `ReminderScheduler` interface.
2. **Create `Reminder` domain model** (commonMain, plain data class — not the raw SQLDelight row).
3. **Create `ReminderRepository`** wrapping both the database and the scheduler.
4. **Create `ReminderListViewModel`** and `AddEditReminderViewModel` with their `UiState` sealed classes.
5. **Wire DI** in `AppModule.kt` (Koin) for repository + both ViewModels.
6. **Validate**
   - Every repository method that changes a reminder also calls the scheduler — confirm no path leaves the DB and the scheduled alarm out of sync.

---

## Detailed Requirements

### `Reminder` domain model (`domain/model/Reminder.kt`)
```kotlin
data class Reminder(
    val id: Long,
    val medicineName: String,
    val dosage: String?,
    val hour: Int,
    val minute: Int,
    val isActive: Boolean
)
```

### `ReminderRepository`
- `fun observeReminders(): Flow<List<Reminder>>` — reactive query from SQLDelight (`selectAll`)
- `suspend fun addReminder(medicineName: String, dosage: String?, hour: Int, minute: Int): Long` — inserts, then calls `scheduler.scheduleReminder(...)` with the new id
- `suspend fun updateReminder(reminder: Reminder)` — updates the row, then re-schedules (cancel + schedule, since time may have changed)
- `suspend fun setActive(id: Long, active: Boolean)` — updates `is_active`, then either schedules or cancels accordingly
- `suspend fun deleteReminder(id: Long)` — deletes the row, then calls `scheduler.cancelReminder(id)`
- `suspend fun checkExactAlarmPermission(): Boolean` — delegates to `scheduler.hasExactAlarmPermission()`, used by the UI to show a permission prompt if needed

### `ReminderListViewModel`
- `UiState`: sealed class with `Loading`, `Loaded(reminders: List<Reminder>, exactAlarmPermissionGranted: Boolean)`, `Empty`
- Collects `repository.observeReminders()` into the `StateFlow`
- Exposes `toggleActive(id: Long)` and `deleteReminder(id: Long)` actions

### `AddEditReminderViewModel`
- `UiState`: holds form fields (`medicineName`, `dosage`, `hour`, `minute`), a `canSave: Boolean` (true only when `medicineName` is non-blank), and a `saved: Boolean` flag for navigation
- `save()` action calls `repository.addReminder(...)` or `repository.updateReminder(...)` depending on whether editing an existing id

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `commonMain/domain/model/Reminder.kt` | domain model |
| CREATE | `commonMain/data/repository/ReminderRepository.kt` | repository |
| CREATE | `commonMain/presentation/reminderlist/ReminderListViewModel.kt` | list ViewModel + UiState |
| CREATE | `commonMain/presentation/addedit/AddEditReminderViewModel.kt` | add/edit ViewModel + UiState |
| MODIFY | `commonMain/core/di/AppModule.kt` | register repository + ViewModels in Koin |

Show only these files.

---

## Constraints

- Do NOT call `ReminderScheduler` or SQLDelight queries from anywhere except `ReminderRepository`.
- Do NOT let any repository method change the database without also updating the scheduler — they must stay in sync on every write path.
- Keep explanations to 2–3 sentences maximum.

---

## Success Criteria

- [ ] `ReminderRepository` is the only class touching both SQLDelight and `ReminderScheduler`
- [ ] Every write path (add/update/delete/toggle) keeps the DB and scheduled alarm consistent
- [ ] Both ViewModels expose `StateFlow<UiState>` with no business logic duplicated in the UI layer
- [ ] Koin module resolves all dependencies with no manual wiring left in the UI
