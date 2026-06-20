# Prompt 3 — Notification / Exact Alarm Scheduling

> **Before starting**: Read `CLAUDE.md` for full project context. Do not proceed without it.

---

## Role

You are a senior Kotlin Multiplatform engineer who has shipped exact-time alarm/notification features on both Android (`AlarmManager`) and iOS (`UNUserNotificationCenter`). This is the core feature of the app — treat reliability as more important than polish.

---

## Goal

A `ReminderScheduler` `expect` interface in `commonMain` has working Android and iOS `actual` implementations that schedule a notification to fire at the exact `hour:minute` every day for a given reminder, cancel it when a reminder is deleted/deactivated, and reschedule all active reminders after an Android device reboot.

---

## Context Snapshot

- **Stack**: `commonMain` expect/actual, `AlarmManager` + `BroadcastReceiver` (Android), `UNUserNotificationCenter` + `UNCalendarNotificationTrigger` (iOS)
- **Relevant existing files**: `sqldelight/com/pilltime/db/Reminder.sq` (prompt 2) — defines the `Reminder` shape this scheduler operates on
- **Upstream dependencies**: prompt 1 (manifest permissions/receivers declared), prompt 2 (reminder data model)
- **Downstream consumers**: `prompt_4_state.md` — `ReminderRepository` calls this scheduler on every insert/update/delete

---

## Task Breakdown

1. **Inspect first**
   - Read the manifest receiver declarations from prompt 1 and the `reminder` table from prompt 2.
2. **Define `ReminderScheduler` expect interface** in `commonMain/core/notification/`.
3. **Implement Android actual**: exact alarm scheduling, permission checks, the alarm-fired receiver, and the boot receiver.
4. **Implement iOS actual**: authorization request, calendar-based repeating trigger.
5. **Add the "Taken" / "Snooze 10 min" notification actions** (P2 — see Detailed Requirements).
6. **Validate**
   - Confirm cancellation logic exists for every scheduling path.
   - Confirm the boot-reschedule path is wired to `BOOT_COMPLETED`.

---

## Detailed Requirements

### `ReminderScheduler` (commonMain, expect)
```kotlin
interface ReminderScheduler {
    suspend fun scheduleReminder(reminder: Reminder)
    suspend fun cancelReminder(reminderId: Long)
    suspend fun rescheduleAll(reminders: List<Reminder>)
    suspend fun hasExactAlarmPermission(): Boolean   // Android: real check; iOS: always true
}
```

### Android actual (P1)
- Use `AlarmManager.setExactAndAllowWhileIdle()` (or the dedicated exact-alarm API) keyed by `reminderId` as the `PendingIntent` request code, so re-scheduling the same id replaces the existing alarm.
- Before scheduling, call `AlarmManager.canScheduleExactAlarms()` (API 31+). If `false`, do **not** silently fall back to an inexact alarm — surface this back to the caller (via the `hasExactAlarmPermission()` check) so the UI layer (prompt 5) can prompt the user to grant it via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- `AlarmReceiver` (a `BroadcastReceiver`): on alarm fire, builds and shows a high-importance `NotificationChannel` notification containing the medicine name/dosage, and re-schedules itself for the next day (since `setExactAndAllowWhileIdle` is one-shot, not repeating).
- `BootReceiver` (a `BroadcastReceiver` on `BOOT_COMPLETED`): reads all active reminders from the database and calls `rescheduleAll()`. This is P1 — without it, every reminder silently stops working after a restart.
- Request `POST_NOTIFICATIONS` at runtime (Android 13+) before the first scheduling attempt.

### iOS actual (P1)
- Request authorization via `UNUserNotificationCenter.requestAuthorization(options: [.alert, .sound, .badge])` once, the first time a reminder is scheduled.
- Use `UNCalendarNotificationTrigger` with `DateComponents(hour:, minute:)` and `repeats: true` — iOS handles the daily repeat natively, no manual re-scheduling needed.
- Use the reminder's database id (as a string) as the `UNNotificationRequest` identifier, so updating/cancelling is a simple remove-by-identifier.

### "Taken" / "Snooze 10 min" notification actions (P2 — add if time allows, do not let this block P1)
- Android: define a `UNNotificationCategory`-equivalent via `NotificationCompat.Action` with two actions on the notification: "Taken" (dismisses, does nothing else for v1) and "Snooze 10 min" (re-schedules a one-shot alarm 10 minutes later via the same `AlarmReceiver`).
- iOS: define a `UNNotificationCategory` with two `UNNotificationAction`s ("Taken", "Snooze 10 min"), registered at app startup, and attached to each scheduled request's `categoryIdentifier`.
- If implementing this, note it explicitly in the prompt 4 handoff — `ReminderRepository` does not need to know about it, this stays entirely inside the scheduler/receiver layer.

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `commonMain/core/notification/ReminderScheduler.kt` | expect interface |
| CREATE | `androidMain/notification/ReminderScheduler.android.kt` | actual impl |
| CREATE | `androidMain/notification/AlarmReceiver.kt` | fires notification, re-arms next day |
| CREATE | `androidMain/notification/BootReceiver.kt` | reschedules all active reminders |
| CREATE | `iosMain/notification/ReminderScheduler.ios.kt` | actual impl |

Show only these files.

---

## Constraints

- Do NOT use inexact alarms (`set()` / `setWindow()`) as the default path — exact timing is the entire point of this feature.
- Do NOT skip the `BootReceiver` — this is the most commonly missed bug in Android alarm apps.
- Do NOT use any push-notification service (FCM/APNs remote push) — everything is scheduled locally.
- Keep explanations to 2–3 sentences maximum.
- If the exact-alarm permission is denied on Android, do not silently degrade — return that state so the UI can act on it.

---

## Success Criteria

- [ ] `ReminderScheduler` expect/actual compiles on both targets
- [ ] Android: alarm fires within seconds of the exact scheduled minute, including when the device is idle/in Doze
- [ ] Android: reminders still fire correctly after a simulated reboot (`BootReceiver` reschedules them)
- [ ] iOS: notification fires daily at the exact scheduled time via `UNCalendarNotificationTrigger`
- [ ] Cancelling/deactivating a reminder removes its pending alarm/notification on both platforms
- [ ] No push-notification dependency was introduced
