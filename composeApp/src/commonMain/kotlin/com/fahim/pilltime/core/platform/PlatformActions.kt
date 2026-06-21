package com.fahim.pilltime.core.platform

/**
 * Opens the OS screen where the user grants the exact-alarm permission. Android navigates to
 * `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; iOS is a no-op (no such permission exists there).
 */
expect fun openExactAlarmSettings()

/**
 * Whether the app is currently allowed to post notifications. Android reflects the runtime
 * `POST_NOTIFICATIONS` grant (API 33+) and the user's notification toggle; iOS always returns
 * true (it requests notification authorization at runtime, so the banner never shows there).
 */
expect fun hasNotificationPermission(): Boolean

/**
 * Opens the OS notification settings for this app so the user can enable notifications.
 * iOS is a no-op (authorization is requested in-app via UNUserNotificationCenter).
 */
expect fun openNotificationSettings()
