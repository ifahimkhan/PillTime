package com.fahim.pilltime.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.fahim.pilltime.core.notification.ReminderScheduler
import com.fahim.pilltime.db.Reminder
import java.util.Calendar

/** Shared constants/keys for the Android alarm + notification plumbing. */
internal object AlarmConst {
    const val CHANNEL_ID = "pilltime_reminders"
    const val CHANNEL_NAME = "Medication reminders"

    // Intent actions handled by AlarmReceiver.
    const val ACTION_FIRE = "com.fahim.pilltime.action.ALARM_FIRE"
    const val ACTION_TAKEN = "com.fahim.pilltime.action.TAKEN"
    const val ACTION_SNOOZE = "com.fahim.pilltime.action.SNOOZE"
    const val ACTION_STOP = "com.fahim.pilltime.action.STOP"

    // Intent extras (carry the whole reminder so receivers never need DB access to re-arm).
    const val EXTRA_ID = "extra_id"
    const val EXTRA_NAME = "extra_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"
    // True for the daily alarm (re-arm for tomorrow on fire); false for a one-shot snooze.
    const val EXTRA_REARM = "extra_rearm"

    const val SNOOZE_MINUTES = 10

    // Distinct request-code spaces so the one-shot snooze alarm never overwrites the daily alarm.
    fun dailyRequestCode(id: Long): Int = id.toInt()
    fun snoozeRequestCode(id: Long): Int = (id + 1_000_000L).toInt()
}

/**
 * Creates the medication-reminder notification channel. Idempotent: `createNotificationChannel`
 * updates a channel in place if it already exists, so this is safe to call on every app start.
 * The channel is created ONCE at startup and never deleted at runtime — repeatedly deleting and
 * recreating a channel makes Android start ignoring its importance/sound settings.
 */
internal fun ensureReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        AlarmConst.CHANNEL_ID,
        AlarmConst.CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Exact-time medication reminders"
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        val soundUri = Uri.parse("android.resource://${context.packageName}/${com.fahim.pilltime.R.raw.alarmed}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        setSound(soundUri, audioAttributes)
    }
    manager.createNotificationChannel(channel)
}

/**
 * Android exact-alarm scheduler. Uses [AlarmManager.setExactAndAllowWhileIdle] keyed by the
 * reminder id so re-scheduling replaces the prior alarm. Does NOT fall back to inexact alarms:
 * if exact-alarm permission is missing it no-ops and reports via [hasExactAlarmPermission] so the
 * UI (prompt 5) can request `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
 */
class AndroidReminderScheduler(
    private val context: Context,
) : ReminderScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleReminder(reminder: Reminder) {
        if (!hasExactAlarmPermission()) return // surfaced to caller; never silently degrade
        val triggerAt = nextTriggerMillis(reminder.hour.toInt(), reminder.minute.toInt())
        scheduleExactAlarm(
            context = context,
            reminder = reminder,
            triggerAtMillis = triggerAt,
            requestCode = AlarmConst.dailyRequestCode(reminder.id),
        )
    }

    override suspend fun cancelReminder(reminderId: Long) {
        // Cancel both the daily alarm and any pending snooze for this id.
        cancelAlarm(context, AlarmConst.dailyRequestCode(reminderId))
        cancelAlarm(context, AlarmConst.snoozeRequestCode(reminderId))
    }

    override suspend fun rescheduleAll(reminders: List<Reminder>) {
        reminders.forEach { scheduleReminder(it) }
    }

    override suspend fun hasExactAlarmPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
}

/** Next epoch-millis at [hour]:[minute] local time — today if still upcoming, else tomorrow. */
internal fun nextTriggerMillis(hour: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    val target = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.timeInMillis <= now.timeInMillis) {
        target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return target.timeInMillis
}

/** Build the PendingIntent that drives AlarmReceiver, carrying the full reminder payload. */
internal fun firePendingIntent(
    context: Context,
    reminder: Reminder,
    requestCode: Int,
    rearm: Boolean = true,
): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmConst.ACTION_FIRE
        putExtra(AlarmConst.EXTRA_ID, reminder.id)
        putExtra(AlarmConst.EXTRA_NAME, reminder.medicine_name)
        putExtra(AlarmConst.EXTRA_DOSAGE, reminder.dosage)
        putExtra(AlarmConst.EXTRA_HOUR, reminder.hour.toInt())
        putExtra(AlarmConst.EXTRA_MINUTE, reminder.minute.toInt())
        putExtra(AlarmConst.EXTRA_REARM, rearm)
    }
    return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Schedule a single exact, Doze-exempt one-shot alarm. */
internal fun scheduleExactAlarm(
    context: Context,
    reminder: Reminder,
    triggerAtMillis: Long,
    requestCode: Int,
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return
    val pi = firePendingIntent(context, reminder, requestCode)
    // setAlarmClock is exempt from Doze and battery-optimization throttling, so alarms scheduled
    // close together (e.g. 5 minutes apart) each fire at their exact time. setExactAndAllowWhileIdle
    // is throttled to ~one alarm per 9-15 min window per app, which silently drops/defers the
    // second of two nearby alarms.
    val info = AlarmManager.AlarmClockInfo(triggerAtMillis, appShowPendingIntent(context))
    alarmManager.setAlarmClock(info, pi)
}

/** PendingIntent shown when the user taps the system alarm indicator — just opens the app. */
internal fun appShowPendingIntent(context: Context): PendingIntent {
    val launch = Intent(context, com.fahim.pilltime.MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    return PendingIntent.getActivity(
        context,
        0,
        launch,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Cancel a scheduled alarm by request code. */
internal fun cancelAlarm(context: Context, requestCode: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )
    if (pi != null) {
        alarmManager.cancel(pi)
        pi.cancel()
    }
}
