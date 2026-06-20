package com.fahim.pilltime.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fahim.pilltime.db.Reminder

/**
 * Fires when an exact alarm goes off. Starts the AlarmService (foreground service) to ring
 * the medication alarm loudly and persistently. Also handles action broadcasts (Taken/Snooze)
 * by stopping the service.
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
                context.stopService(Intent(context, AlarmService::class.java))
            }

            AlarmConst.ACTION_SNOOZE -> {
                context.stopService(Intent(context, AlarmService::class.java))
                
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
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(AlarmConst.EXTRA_ID, id)
                    putExtra(AlarmConst.EXTRA_NAME, name)
                    putExtra(AlarmConst.EXTRA_DOSAGE, dosage)
                    putExtra(AlarmConst.EXTRA_HOUR, hour)
                    putExtra(AlarmConst.EXTRA_MINUTE, minute)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

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
}
