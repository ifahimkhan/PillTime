package com.fahim.pilltime.core.platform

/**
 * Opens the OS screen where the user grants the exact-alarm permission. Android navigates to
 * `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; iOS is a no-op (no such permission exists there).
 */
expect fun openExactAlarmSettings()
