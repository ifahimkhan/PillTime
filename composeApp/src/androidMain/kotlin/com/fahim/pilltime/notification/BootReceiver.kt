package com.fahim.pilltime.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fahim.pilltime.core.notification.ReminderScheduler
import com.fahim.pilltime.db.PillTimeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Reschedules every active reminder after a device reboot. AlarmManager alarms do NOT survive a
 * restart, so without this every reminder silently stops firing — this is P1, not optional.
 *
 * Resolves the database + scheduler from Koin (both registered in prompt 4's AppModule). The app
 * process Application.onCreate (which calls startKoin) runs before this receiver fires.
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val database: PillTimeDatabase by inject()
    private val scheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Keep the process alive while the DB read + (re)scheduling completes.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val active = database.reminderQueries.selectActive().executeAsList()
                scheduler.rescheduleAll(active)
            } finally {
                pending.finish()
            }
        }
    }
}
