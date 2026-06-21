package com.fahim.pilltime.core.platform

// iOS has no exact-alarm permission; the banner that triggers this never shows on iOS
// (hasExactAlarmPermission() always returns true there).
actual fun openExactAlarmSettings() = Unit

// iOS requests notification authorization at runtime via UNUserNotificationCenter, so the
// Android-style "enable notifications" banner is not used here.
actual fun hasNotificationPermission(): Boolean = true

actual fun openNotificationSettings() = Unit
