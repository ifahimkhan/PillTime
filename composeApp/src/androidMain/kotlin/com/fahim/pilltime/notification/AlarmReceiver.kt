package com.fahim.pilltime.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fahim.pilltime.db.Reminder

/**
 * Fires when an exact alarm goes off. Shows a high-importance notification for the medicine,
 * then re-arms the daily alarm for the next day (setExactAndAllowWhileIdle is one-shot, not
 * repeating). Also handles the P2 "Taken" / "Snooze 10 min" notification actions.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmConst.EXTRA_ID, -1L)
        if (id < 0) return
        val name = intent.getStringExtra(AlarmConst.EXTRA_NAME).orEmpty()
        val dosage = intent.getStringExtra(AlarmConst.EXTRA_DOSAGE)
        val hour = intent.getIntExtra(AlarmConst.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmConst.EXTRA_MINUTE, 0)
        val reminder = Reminder(
            id = id,
            medicine_name = name,
            dosage = dosage,
            hour = hour.toLong(),
            minute = minute.toLong(),
            is_active = 1L,
            created_at = 0L,
        )

        when (intent.action) {
            AlarmConst.ACTION_TAKEN -> {
                // v1: just dismiss the notification.
                NotificationManagerCompat.from(context).cancel(id.toInt())
            }

            AlarmConst.ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(id.toInt())
                // One-shot alarm 10 min later; rearm=false so it won't touch the daily schedule.
                val pi = firePendingIntent(
                    context = context,
                    reminder = reminder,
                    requestCode = AlarmConst.snoozeRequestCode(id),
                    rearm = false,
                )
                val triggerAt = System.currentTimeMillis() + AlarmConst.SNOOZE_MINUTES * 60_000L
                val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pi,
                    )
                }
            }

            else -> { // ACTION_FIRE
                ensureChannel(context)
                showNotification(context, reminder)
                // Re-arm for the next day only for the daily alarm, not for snooze one-shots.
                if (intent.getBooleanExtra(AlarmConst.EXTRA_REARM, true)) {
                    scheduleExactAlarm(
                        context = context,
                        reminder = reminder,
                        triggerAtMillis = nextTriggerMillis(hour, minute),
                        requestCode = AlarmConst.dailyRequestCode(id),
                    )
                }
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AlarmConst.CHANNEL_ID,
                AlarmConst.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Exact-time medication reminders" }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, reminder: Reminder) {
        val title = "Time to take ${reminder.medicine_name}"
        val text = reminder.dosage?.takeIf { it.isNotBlank() }?.let { "Dosage: $it" }
            ?: "Tap when taken"

        val takenPi = actionPendingIntent(context, reminder, AlarmConst.ACTION_TAKEN, code = 1)
        val snoozePi = actionPendingIntent(context, reminder, AlarmConst.ACTION_SNOOZE, code = 2)

        val notification = NotificationCompat.Builder(context, AlarmConst.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(0, "Taken", takenPi)
            .addAction(0, "Snooze 10 min", snoozePi)
            .build()

        // POST_NOTIFICATIONS (Android 13+) is requested by the UI layer (prompt 5); if it was
        // denied, notify() is a silent no-op rather than a crash.
        try {
            NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
        } catch (_: SecurityException) {
            // permission not granted yet — ignored
        }
    }

    private fun actionPendingIntent(
        context: Context,
        reminder: Reminder,
        action: String,
        code: Int,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmConst.EXTRA_ID, reminder.id)
            putExtra(AlarmConst.EXTRA_NAME, reminder.medicine_name)
            putExtra(AlarmConst.EXTRA_DOSAGE, reminder.dosage)
            putExtra(AlarmConst.EXTRA_HOUR, reminder.hour.toInt())
            putExtra(AlarmConst.EXTRA_MINUTE, reminder.minute.toInt())
        }
        // Unique request code per (reminder, action) so the two actions don't collide.
        return PendingIntent.getBroadcast(
            context,
            (reminder.id.toInt() * 10) + code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
