package com.fahim.pilltime.notification

import com.fahim.pilltime.core.notification.ReminderScheduler
import com.fahim.pilltime.db.Reminder
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

private const val CATEGORY_ID = "PILLTIME_REMINDER"
private const val ACTION_TAKEN = "TAKEN"
private const val ACTION_SNOOZE = "SNOOZE"

/**
 * iOS scheduler backed by UNUserNotificationCenter. Uses a UNCalendarNotificationTrigger with
 * `repeats = true`, so iOS handles the daily repeat natively (no manual re-arming). The reminder
 * id (as a string) is the request identifier, making update/cancel a simple remove-by-identifier.
 */
class IosReminderScheduler : ReminderScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private var prepared = false

    override suspend fun scheduleReminder(reminder: Reminder) {
        prepareOnce()

        val content = UNMutableNotificationContent().apply {
            setTitle("Time to take ${reminder.medicine_name}")
            setBody(reminder.dosage?.takeIf { it.isNotBlank() }?.let { "Dosage: $it" } ?: "Tap when taken")
            setSound(UNNotificationSound.soundNamed("Alarmed.wav"))
            setCategoryIdentifier(CATEGORY_ID)
        }

        val components = NSDateComponents().apply {
            hour = reminder.hour
            minute = reminder.minute
            second = 0
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = components,
            repeats = true,
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = reminder.id.toString(),
            content = content,
            trigger = trigger,
        )
        // Re-adding the same identifier replaces the previous request, so update == reschedule.
        center.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error scheduling iOS notification: ${error.localizedDescription}")
            }
        }
    }

    override suspend fun cancelReminder(reminderId: Long) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(reminderId.toString()))
    }

    override suspend fun rescheduleAll(reminders: List<Reminder>) {
        reminders.forEach { scheduleReminder(it) }
    }

    // iOS has no exact-alarm permission concept; calendar triggers fire at the exact minute.
    override suspend fun hasExactAlarmPermission(): Boolean = true

    /** One-time: request notification authorization and register the Taken/Snooze category. */
    private fun prepareOnce() {
        if (prepared) return
        prepared = true

        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { _, _ -> /* result handled at UI layer */ }

        val taken = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_TAKEN,
            title = "Taken",
            options = 0UL,
        )
        val snooze = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_SNOOZE,
            title = "Snooze 10 min",
            options = 0UL,
        )
        val category = UNNotificationCategory.categoryWithIdentifier(
            identifier = CATEGORY_ID,
            actions = listOf(taken, snooze),
            intentIdentifiers = emptyList<String>(),
            options = UNNotificationCategoryOptionNone,
        )
        center.setNotificationCategories(setOf(category))
    }
}
