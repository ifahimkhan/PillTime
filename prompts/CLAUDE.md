# CLAUDE.md — PillTime

> This file is the persistent memory for Claude Code across all sessions.
> Read this before starting any task. Do not skip it.

---

## App Identity

- **Name**: PillTime (rename freely — just keep this file in sync if you do)
- **Purpose**: Let a user add medication reminders and get notified/alarmed at the exact scheduled time, every day.
- **Target users**: Anyone taking scheduled medication who needs a reliable, exact-time reminder.
- **Platform**: Android + iOS via Kotlin Multiplatform + Compose Multiplatform
- **Status**: Greenfield

---

## Tech Stack

### Frontend
- Framework: Compose Multiplatform (shared UI in `commonMain`)
- Language: Kotlin
- State management: ViewModel + StateFlow (kotlinx.coroutines)
- Navigation: Compose Navigation, centralized in `AppNavGraph.kt`
- Design system: Material 3, no custom branding for v1

### Backend
- **None.** This is a fully offline, local-only app. No server, no sync, no accounts.

### Local Data
- Database: SQLDelight (multiplatform local SQL database)
- Scheduling: Platform-native exact alarm / local notification APIs via `expect`/`actual`

### Shared / Infra
- Monorepo: no (single KMP module, `composeApp`)
- DI: Koin (multiplatform)
- Package manager: Gradle
- Date/time: kotlinx-datetime

### Pinned versions (set in `gradle/libs.versions.toml`, prompt_1)
- Kotlin `2.4.0`, AGP `9.0.1`, Compose Multiplatform `1.11.1`, Material3 `1.11.0-alpha07`
- SQLDelight `2.1.0` (+ `coroutines-extensions`), Koin `4.1.0` (+ `koin-compose`,
  `koin-compose-viewmodel` for `koinViewModel()` in Composables — added in prompt_5)
- navigation-compose `2.9.0`, kotlinx-datetime `0.6.2`, kotlinx-coroutines `1.10.2`
- Timestamps use stdlib `kotlin.time.Clock` (Kotlin 2.4 stabilized it; `kotlinx.datetime.Clock`
  fails to resolve `.System` on Kotlin/Native here), opted in with `@OptIn(ExperimentalTime::class)`
- androidx-core `1.15.0` (pinned down from 1.19.0, which requires AGP 9.1.0 / compileSdk 37)
- Root package: `com.fahim.pilltime` (SQLDelight db package: `com.fahim.pilltime.db`)

### Build/scaffold notes (prompt_1)
- `composeApp` is a single module that is **both** the Android application and the KMP
  module. AGP 9.0 forbids `com.android.application` + `kotlinMultiplatform` under the new
  built-in Kotlin/DSL, so `android.builtInKotlin=false` and `android.newDsl=false` are set
  in `gradle.properties` to opt back into legacy behavior.
- iOS targets: `iosArm64` + `iosSimulatorArm64` only. `iosX64` (Intel simulator) is omitted
  because Compose Multiplatform 1.11.x no longer publishes `iosX64` variants.
- The iOS framework `baseName` is kept as `Shared` so `iosApp` needs no Swift changes; the
  Xcode "Compile Kotlin Framework" phase calls `:composeApp:embedAndSignAppleFrameworkForXcode`.

### Prompt 3 → 4 handoff (DI + permissions the next prompt must wire)
- The scheduler operates directly on the SQLDelight `com.fahim.pilltime.db.Reminder` row (no
  separate domain model was introduced). `ReminderRepository` should accept a `ReminderScheduler`
  and call `scheduleReminder` / `cancelReminder` on every insert/update/delete.
- **Koin (prompt 4 `AppModule`) must register**: `PillTimeDatabase` (single), `ReminderScheduler`
  → `AndroidReminderScheduler(androidContext())` on Android and `IosReminderScheduler()` on iOS.
  `BootReceiver` resolves `PillTimeDatabase` + `ReminderScheduler` via Koin, so `startKoin` must
  run in the Android `Application.onCreate` (prompt 4 also creates that Application + registers it
  in the manifest).
- **UI (prompt 5) must request** runtime `POST_NOTIFICATIONS` (Android 13+) and, when
  `hasExactAlarmPermission()` is false, send the user to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
  The scheduler never silently degrades to inexact alarms.
- P2 "Taken" / "Snooze 10 min" actions are implemented entirely inside the scheduler/receiver
  layer (Android `AlarmReceiver` action handlers; iOS `UNNotificationCategory`). The repository
  does not need to know about them.

---

## Project Structure

```
PillTime/
├── composeApp/
│   ├── commonMain/
│   │   ├── data/
│   │   │   ├── local/              # SQLDelight database driver factory (expect)
│   │   │   └── repository/         # ReminderRepository.kt
│   │   ├── domain/
│   │   │   └── model/              # Reminder.kt
│   │   ├── presentation/
│   │   │   ├── reminderlist/       # ReminderListScreen, ReminderListViewModel
│   │   │   └── addedit/            # AddEditReminderScreen, AddEditReminderViewModel
│   │   └── core/
│   │       ├── navigation/         # AppNavGraph.kt
│   │       ├── di/                 # AppModule.kt
│   │       └── notification/       # ReminderScheduler.kt (expect interface)
│   ├── androidMain/
│   │   ├── data/local/             # DatabaseDriverFactory.android.kt
│   │   └── notification/           # ReminderScheduler.android.kt, AlarmReceiver.kt, BootReceiver.kt
│   └── iosMain/
│       ├── data/local/             # DatabaseDriverFactory.ios.kt
│       └── notification/           # ReminderScheduler.ios.kt
├── sqldelight/
│   └── com/fahim/pilltime/db/Reminder.sq
└── CLAUDE.md
```

---

## Domain Ownership Map

| Domain | Directory / Module | Primary files |
|--------|--------------------|--------------|
| Database / Schema | `sqldelight/com/fahim/pilltime/db/` | `Reminder.sq` |
| Notification / Alarm scheduling | `core/notification/`, `androidMain/notification/`, `iosMain/notification/` | `ReminderScheduler.kt` (+ actuals), `AlarmReceiver.kt`, `BootReceiver.kt` |
| State / Data layer | `data/repository/`, `presentation/*/` | `ReminderRepository.kt`, `*ViewModel.kt` |
| UI screens | `presentation/*/` | `*Screen.kt` |

---

## Architecture Decisions

- All state lives in ViewModels, never in Composables directly.
- No direct SQLDelight or AlarmManager/UNUserNotificationCenter calls from Composables — always through `ReminderRepository`.
- `ReminderRepository` is the single source of truth: every insert/update/delete of a reminder also schedules/cancels the underlying alarm via `ReminderScheduler`, so UI code never calls the scheduler directly.
- `ReminderScheduler` is an `expect` interface in `commonMain`; Android and iOS each provide an `actual` implementation using native exact-alarm / local-notification APIs.
- Android: alarms must be rescheduled on device boot (`BootReceiver`) since `AlarmManager` alarms do not survive a reboot.
- Navigation is centralized in `AppNavGraph.kt` — no scattered NavController calls.

---

## Naming Conventions

| Entity | Convention | Example |
|--------|-----------|---------|
| Screens | `[Name]Screen.kt` | `ReminderListScreen.kt` |
| ViewModels | `[Name]ViewModel.kt` | `ReminderListViewModel.kt` |
| Repositories | `[Name]Repository.kt` | `ReminderRepository.kt` |
| DB tables | `snake_case` | `reminder` |
| Models | plain data class, `commonMain/domain/model/` | `Reminder.kt` |
| Scheduler | `ReminderScheduler` (expect) / `ReminderScheduler.android.kt`, `.ios.kt` (actual) | — |

---

## Database Schema Overview

> Full schema in `sqldelight/com/fahim/pilltime/db/Reminder.sq`. Summary only.

| Table | Purpose |
|-------|---------|
| `reminder` | One row per pill reminder: medicine name, dosage, time of day, active flag |

No RLS — this is a local-only single-user database, not a remote multi-tenant one.

---

## Auth Model

- **None.** No accounts, no login, no remote identity. All data is local to the device.

---

## Permissions Required

| Permission | Platform | Why |
|-----------|----------|-----|
| `POST_NOTIFICATIONS` | Android 13+ | Required at runtime just to display the notification |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Android 12+ | Required to fire alarms at the exact scheduled minute, even in Doze |
| `RECEIVE_BOOT_COMPLETED` | Android | Required to reschedule alarms after device restart |
| Local notification authorization (`.alert`, `.sound`, `.badge`) | iOS | Required before any `UNUserNotificationCenter` notification can be shown |

---

## Critical Rules for Claude Code

### Always
- Read relevant files before writing any code.
- Preserve the existing architecture and naming conventions.
- Treat the exact-alarm and reboot-rescheduling logic as P1 — this is the core feature, not a nice-to-have.
- Show only changed/created files in output.
- Keep explanations to 2–3 sentences max.

### Never
- Rewrite unrelated code.
- Call SQLDelight or platform alarm/notification APIs directly from a Composable — always go through `ReminderRepository`.
- Add dependencies without noting them here.
- Assume exact-alarm permission is granted on Android 12+ — always check and handle the "not granted" path.
- Skip the boot-rescheduling logic — without it, alarms silently stop working after every restart.

---

## Prompt File Index

> These are the Claude Code prompts for this project, in execution order.

| # | File | Domain | Status | Depends on |
|---|------|--------|--------|-----------|
| 1 | `prompt_1_architecture.md` | Architecture / scaffold | [x] done | — |
| 2 | `prompt_2_database.md` | Local database (SQLDelight) | [x] done | 1 |
| 3 | `prompt_3_notifications.md` | Notification / exact alarm scheduling | [x] done | 1, 2 |
| 4 | `prompt_4_state.md` | Repository + ViewModels | [x] done | 2, 3 |
| 5 | `prompt_5_ui.md` | UI screens | [x] done | 4 |

Mark `[x] done` as each prompt is completed.

---

## Change Log

| Date | Change | Prompt |
|------|--------|--------|
| 2026-06-20 | Initial scaffold | prompt_1 |
| 2026-06-21 | Restructured wizard `shared`+`androidApp` into single `composeApp` module; added SQLDelight/Koin/nav/datetime deps; permissions + AlarmReceiver/BootReceiver declared; verified Android `assembleDebug` + iOS framework link | prompt_1 |
| 2026-06-21 | Added `Reminder.sq` (single `reminder` table + 7 typed queries: insert/selectAll/selectActive/selectById/update/setActive/delete); SQLDelight codegen + Android compile verified | prompt_2 |
| 2026-06-21 | `ReminderScheduler` interface (commonMain) + Android (`AlarmManager.setExactAndAllowWhileIdle`, `AlarmReceiver`, `BootReceiver`) and iOS (`UNCalendarNotificationTrigger`, repeats=true) impls; P2 Taken/Snooze actions on both; compiles Android + both iOS targets | prompt_3 |
| 2026-06-21 | Repository + ViewModels: domain `Reminder` model, `ReminderRepository` (reactive `selectAll` flow; every write keeps the scheduler in sync), `ReminderListViewModel` + `AddEditReminderViewModel` (StateFlow). Added DI plumbing the registration needs: `DatabaseDriverFactory` (expect/actual), `AppModule` (`sharedModule` + expect `platformModule` + `initKoin`), `PillTimeApplication` (startKoin, registered in manifest), iOS `doInitKoin()`. Added `lastInsertRowId` query to `Reminder.sq`. Verified compile on all 3 targets + `assembleDebug` APK | prompt_4 |
| 2026-06-21 | UI: `ReminderListScreen` + `AddEditReminderScreen` (Material 3, capsule design from design.md), `AppNavGraph` (centralized routes, optional id arg). Added `PillTimeTheme`/tokens, `openExactAlarmSettings()` (expect/actual) for the permission banner, `POST_NOTIFICATIONS` request in `MainActivity`. Added `koin-compose`/`koin-compose-viewmodel` deps. Screens split into stateless `*Content` composables with `@Preview` (loaded/empty/add-edit). Verified compile on all 3 targets + APK | prompt_5 |
