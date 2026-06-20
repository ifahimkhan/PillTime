# Prompt 5 — UI / Screens

> **Before starting**: Read `CLAUDE.md` for full project context. Do not proceed without it.

---

## Role

You are a senior Compose Multiplatform UI engineer building a small, clean Material 3 UI.

---

## Goal

Two working screens — a reminder list and an add/edit form — connected via Compose Navigation, fully driven by the ViewModels from prompt 4, with no business logic in the Composables.

---

## Context Snapshot

- **Design system**: Material 3, no custom branding for v1
- **Navigation**: Compose Navigation, centralized in `AppNavGraph.kt`
- **State sources**: `ReminderListViewModel`, `AddEditReminderViewModel` (prompt 4) via `collectAsStateWithLifecycle()`
- **Platform**: Android + iOS (shared Compose UI in `commonMain`)

---

## Task Breakdown

1. **Inspect first**
   - Read both ViewModels' `UiState` shapes from prompt 4.
2. **Build `ReminderListScreen`**.
3. **Build `AddEditReminderScreen`**.
4. **Wire `AppNavGraph.kt`** connecting the two screens.
5. **Handle the exact-alarm permission prompt** (Android-only edge case — see Detailed Requirements).
6. **Validate**
   - Confirm no Composable reads SQLDelight or the scheduler directly — only ViewModel state.

---

## Detailed Requirements

### `ReminderListScreen`
- Collects `ReminderListViewModel`'s `UiState` via `collectAsStateWithLifecycle()`.
- `Empty` state: centered text + icon prompting the user to add their first reminder.
- `Loaded` state: a scrollable list of reminder cards, each showing medicine name, dosage (if present), time formatted as `h:mm a`, and a `Switch` bound to `isActive` that calls `toggleActive(id)`.
- Swipe-to-delete or a trailing delete icon per row, calling `deleteReminder(id)`.
- A `FloatingActionButton` navigating to the add screen.
- If `exactAlarmPermissionGranted` is `false` (Android only), show a dismissible banner explaining that exact reminders require a permission, with a button that opens the system settings screen for it.

### `AddEditReminderScreen`
- Text field for medicine name (required).
- Optional text field for dosage (e.g. "1 tablet", "10mg").
- A time picker for hour/minute (use Compose Multiplatform's Material 3 `TimePicker`).
- Save button, disabled while `canSave` is `false`.
- On `saved == true`, navigate back to the list.

### `AppNavGraph.kt`
- Two routes: `reminderList` (start destination) and `addEditReminder/{id?}` (nullable id for edit vs add).
- Use typed Compose Navigation routes, not raw string concatenation.

### Anti-goals
- No onboarding flow, no settings screen, no themeing beyond default Material 3 — keep v1 scoped to exactly these two screens.
- No loading spinners that block longer than the initial DB read — this is a local DB, state should be near-instant.

### Accessibility
- All interactive elements (switch, delete icon, FAB, save button) have a `contentDescription`.
- Minimum touch target 48dp.

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `commonMain/presentation/reminderlist/ReminderListScreen.kt` | list screen |
| CREATE | `commonMain/presentation/addedit/AddEditReminderScreen.kt` | add/edit screen |
| MODIFY | `commonMain/core/navigation/AppNavGraph.kt` | route wiring |

Show only these files.

---

## Constraints

- Do NOT put any database, repository, or scheduler calls inside a Composable — only ViewModel function calls.
- Do NOT add screens beyond these two for v1.
- Keep explanations to 2–3 sentences maximum.

---

## Success Criteria

- [ ] List screen correctly renders `Empty` and `Loaded` states
- [ ] Toggling, deleting, and adding a reminder all work end-to-end through the ViewModels
- [ ] Time picker produces a valid hour/minute saved through `AddEditReminderViewModel`
- [ ] Android exact-alarm permission banner appears only when the permission is actually missing
- [ ] No Composable directly touches SQLDelight or `ReminderScheduler`
