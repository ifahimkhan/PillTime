# PillTime

A simple, fully offline medication-reminder app for **Android and iOS**, built with Kotlin
Multiplatform and Compose Multiplatform. Add a medicine, a dosage, and a time of day — PillTime
fires an **exact-time** local reminder every day. No accounts, no network, no cloud.

> The one promise: the right pill at the right exact minute.

---

## Features

- Add, edit, pause, and delete daily medication reminders.
- **Exact-time** alarms that fire even in Doze (Android `setExactAndAllowWhileIdle`) and via native
  calendar triggers on iOS — the app never silently falls back to inexact alarms.
- Alarms are **rescheduled after a device reboot** on Android (`BootReceiver`).
- Notification actions: **Taken** and **Snooze 10 min** on both platforms.
- Clean Material 3 UI with a capsule/pill visual language (see `prompts/design.md`).
- 100% local: a single-user on-device SQLite database, no backend, no auth, no push service.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Kotlin `2.4.0` |
| UI | Compose Multiplatform `1.11.1`, Material 3 |
| Platforms | Android (`com.android.application`) + iOS (`iosArm64`, `iosSimulatorArm64`) |
| Local database | SQLDelight `2.1.0` |
| DI | Koin `4.1.0` (+ `koin-compose-viewmodel`) |
| Navigation | navigation-compose `2.9.0` |
| Async / time | kotlinx-coroutines `1.10.2`, kotlinx-datetime `0.6.2` |
| Build | Gradle (version catalog), AGP `9.0.1` |

All notifications are **local/scheduled** — there is no Firebase Cloud Messaging or remote push.

---

## Architecture

Single `composeApp` module that is both the Android application and the shared KMP module.

```
UI (Compose)  ──▶  ViewModel (StateFlow)  ──▶  ReminderRepository  ──┬──▶  SQLDelight (DB)
                                                                     └──▶  ReminderScheduler (alarms)
```

- **`ReminderRepository` is the single source of truth.** Every insert/update/delete also schedules
  or cancels the underlying alarm, so the database and the scheduled reminder never drift apart.
  Composables never touch SQLDelight or the scheduler directly.
- **`ReminderScheduler`** is a common interface with platform `actual` implementations:
  - Android — `AlarmManager` exact alarms + `BroadcastReceiver`s (`AlarmReceiver`, `BootReceiver`).
  - iOS — `UNUserNotificationCenter` with a `UNCalendarNotificationTrigger` (`repeats = true`).
- **DI** is wired in `core/di/AppModule.kt` (`sharedModule` + an `expect`/`actual` `platformModule`),
  started from `PillTimeApplication` on Android and `doInitKoin()` on iOS.

### Project structure

```
composeApp/
  src/commonMain/kotlin/com/fahim/pilltime/
    core/{di, navigation, notification, platform, ui}
    data/{local, repository}
    domain/model
    presentation/{reminderlist, addedit}
  src/androidMain/...    # Android scheduler, receivers, driver, Application
  src/iosMain/...        # iOS scheduler, driver, Koin init
sqldelight/com/fahim/pilltime/db/Reminder.sq   # schema + typed queries
iosApp/                  # Xcode project (iOS entry point + SwiftUI host)
prompts/                 # CLAUDE.md, design.md, build prompts (project history)
```

---

## Permissions

| Permission | Platform | Why |
|---|---|---|
| `POST_NOTIFICATIONS` | Android 13+ | Display the reminder notification (requested at launch). |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Android 12+ | Fire alarms at the exact minute, even in Doze. |
| `RECEIVE_BOOT_COMPLETED` | Android | Reschedule alarms after a restart. |
| Local notification authorization | iOS | Required before any local notification can be shown. |

If the exact-alarm permission is missing on Android 12+, the list screen shows a banner that opens
the system settings screen — reminders are not scheduled until it is granted.

---

## Building & Running

### Requirements
- JDK 21 (the Android Studio bundled JBR works well).
- Android Studio (latest) for Android; Xcode for iOS.

### Android

```bash
# Uses the JDK bundled with Android Studio
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew :composeApp:assembleDebug
# APK -> composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Or open the project in Android Studio and run the `composeApp` configuration on a device/emulator.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device. The Kotlin framework is
built automatically by the `:composeApp:embedAndSignAppleFrameworkForXcode` build phase.

---

## Notes

- `prompts/CLAUDE.md` holds the living architecture notes, pinned versions, and change log.
- This app is intentionally scoped to two screens (reminder list + add/edit) for v1 — no onboarding,
  settings, or history.
