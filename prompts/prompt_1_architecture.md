# Prompt 1 — Architecture / Project Scaffold

> **Before starting**: Read `CLAUDE.md` for full project context. Do not proceed without it.

---

## Role

You are a senior Kotlin Multiplatform engineer setting up a new KMP + Compose Multiplatform project from scratch.

---

## Goal

A buildable KMP project named `PillTime` targeting Android and iOS exists, with all required dependencies declared, the folder structure from `CLAUDE.md` created (empty packages are fine), and platform permissions/manifest entries in place for notifications and exact alarms.

---

## Context Snapshot

- **Stack**: Compose Multiplatform, SQLDelight, Koin, Compose Navigation, kotlinx-datetime
- **Platforms**: Android + iOS
- **No backend, no auth** — this is a fully offline app
- **Downstream dependents**: every other prompt file assumes this scaffold exists

---

## Task Breakdown

1. **Inspect first**
   - Confirm no existing project files conflict with this scaffold.
2. **Initialize the KMP project**
   - Module name: `composeApp`
   - Targets: `androidTarget()`, `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`
3. **Add dependencies** (see Detailed Requirements)
4. **Create the folder structure** exactly as listed in `CLAUDE.md` under "Project Structure", with placeholder files where needed so the structure is visible.
5. **Configure Android manifest and iOS permissions** (see Detailed Requirements)
6. **Validate**
   - Project builds for both Android and iOS targets.
   - Confirm permissions/manifest entries match `CLAUDE.md`'s Permissions table exactly.

---

## Detailed Requirements

### Gradle dependencies (`commonMain`)
- `org.jetbrains.compose` (Compose Multiplatform)
- `app.cash.sqldelight:runtime`
- `io.insert-koin:koin-core`
- `org.jetbrains.compose.navigation` (Compose Navigation multiplatform)
- `org.jetbrains.kotlinx:kotlinx-datetime`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`

### Gradle dependencies (`androidMain`)
- `app.cash.sqldelight:android-driver`
- `io.insert-koin:koin-android`
- AndroidX `core-ktx`, Activity Compose

### Gradle dependencies (`iosMain`)
- `app.cash.sqldelight:native-driver`

### Android Manifest entries (`androidMain/AndroidManifest.xml`)
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```
Also declare placeholder `<receiver>` entries for `AlarmReceiver` and `BootReceiver` (implemented in prompt 3), with `BootReceiver` listening for `android.intent.action.BOOT_COMPLETED`.

### iOS
- No `Info.plist` usage-description key is required for local notifications, but note in code comments that `UNUserNotificationCenter` authorization must be requested at runtime (handled in prompt 3).

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `settings.gradle.kts`, `build.gradle.kts` (root + `composeApp`) | Module + target + dependency config |
| CREATE | `composeApp/androidMain/AndroidManifest.xml` | Permissions + receiver declarations |
| CREATE | Empty package structure per `CLAUDE.md` | Placeholder `.kt` files or `.gitkeep` |

Show only these files. Do not show generated Gradle wrapper files.

---

## Constraints

- Do NOT add a backend, network client, or auth library — this app is fully offline.
- Do NOT add Firebase Cloud Messaging or any push-notification dependency — all notifications are local/scheduled, not pushed.
- Keep explanations to 2–3 sentences maximum.
- If a dependency version is ambiguous, pick the latest stable and note it in `CLAUDE.md`'s Tech Stack section.

---

## Success Criteria

- [ ] Project builds for Android and iOS targets
- [ ] All dependencies from "Detailed Requirements" are declared
- [ ] `AndroidManifest.xml` contains exactly the 4 permissions listed in `CLAUDE.md`
- [ ] Folder structure matches `CLAUDE.md` exactly
- [ ] No backend, auth, or push-notification dependencies were added
