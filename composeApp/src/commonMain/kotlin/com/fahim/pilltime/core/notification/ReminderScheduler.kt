package com.fahim.pilltime.core.notification

import com.fahim.pilltime.db.Reminder

/**
 * Schedules/cancels the exact-time daily notification for a reminder.
 *
 * One interface in commonMain, with platform `actual`-style implementations provided per
 * platform (Android via AlarmManager, iOS via UNUserNotificationCenter) and wired through Koin
 * in prompt 4. Operates on the SQLDelight-generated [Reminder] row directly — local-only app,
 * so no separate domain mapping is needed here.
 */
interface ReminderScheduler {
    /** Schedule [reminder] to fire every day at its `hour:minute`. Replaces any existing
     *  schedule for the same reminder id. No-op if exact-alarm permission is missing (Android). */
    suspend fun scheduleReminder(reminder: Reminder)

    /** Cancel the pending alarm/notification for [reminderId]. Safe to call if none exists. */
    suspend fun cancelReminder(reminderId: Long)

    /** Reschedule a full set of reminders (used by Android's BootReceiver after a reboot). */
    suspend fun rescheduleAll(reminders: List<Reminder>)

    /** Android: real `AlarmManager.canScheduleExactAlarms()` check. iOS: always true.
     *  The UI layer (prompt 5) uses this to prompt for the exact-alarm permission when false. */
    suspend fun hasExactAlarmPermission(): Boolean
}
