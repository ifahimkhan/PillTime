package com.fahim.pilltime.core.platform

// iOS has no exact-alarm permission; the banner that triggers this never shows on iOS
// (hasExactAlarmPermission() always returns true there).
actual fun openExactAlarmSettings() = Unit
