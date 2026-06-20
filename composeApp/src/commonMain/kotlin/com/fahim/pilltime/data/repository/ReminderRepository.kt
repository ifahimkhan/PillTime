package com.fahim.pilltime.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.fahim.pilltime.core.notification.ReminderScheduler
import com.fahim.pilltime.db.PillTimeDatabase
import com.fahim.pilltime.domain.model.Reminder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.fahim.pilltime.db.Reminder as ReminderRow

/**
 * Single source of truth for reminders. Every write path mutates the SQLDelight row AND keeps the
 * platform [ReminderScheduler] in sync, so the database and the scheduled alarm never drift apart.
 * This is the only class permitted to touch SQLDelight or the scheduler (see CLAUDE.md).
 */
class ReminderRepository(
    private val database: PillTimeDatabase,
    private val scheduler: ReminderScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val queries = database.reminderQueries

    /** Reactive list of all reminders (active + paused), ordered by time of day. */
    fun observeReminders(): Flow<List<Reminder>> =
        queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    /** One-shot read of a single reminder, used to prefill the edit form. */
    suspend fun getReminder(id: Long): Reminder? = withContext(ioDispatcher) {
        queries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    /** Insert a reminder, then schedule its alarm. Returns the new row id. */
    suspend fun addReminder(
        medicineName: String,
        dosage: String?,
        hour: Int,
        minute: Int,
    ): Long = withContext(ioDispatcher) {
        @OptIn(ExperimentalTime::class)
        val createdAt = Clock.System.now().toEpochMilliseconds()
        val cleanDosage = dosage?.takeIf { it.isNotBlank() }
        val newId = queries.transactionWithResult {
            queries.insertReminder(medicineName, cleanDosage, hour.toLong(), minute.toLong(), createdAt)
            queries.lastInsertRowId().executeAsOne()
        }
        // New reminders start active, so always schedule.
        scheduler.scheduleReminder(
            ReminderRow(newId, medicineName, cleanDosage, hour.toLong(), minute.toLong(), 1L, createdAt),
        )
        newId
    }

    /** Update a reminder; the time may have changed, so cancel then reschedule (if still active). */
    suspend fun updateReminder(reminder: Reminder) = withContext(ioDispatcher) {
        val cleanDosage = reminder.dosage?.takeIf { it.isNotBlank() }
        queries.updateReminder(
            reminder.medicineName,
            cleanDosage,
            reminder.hour.toLong(),
            reminder.minute.toLong(),
            reminder.id,
        )
        scheduler.cancelReminder(reminder.id)
        if (reminder.isActive) scheduler.scheduleReminder(reminder.copy(dosage = cleanDosage).toRow())
    }

    /** Toggle active/paused; schedule when activating, cancel when pausing. */
    suspend fun setActive(id: Long, active: Boolean) = withContext(ioDispatcher) {
        queries.setActive(if (active) 1L else 0L, id)
        if (active) {
            queries.selectById(id).executeAsOneOrNull()?.let { scheduler.scheduleReminder(it) }
        } else {
            scheduler.cancelReminder(id)
        }
    }

    /** Delete a reminder and cancel its alarm. */
    suspend fun deleteReminder(id: Long) = withContext(ioDispatcher) {
        queries.deleteReminder(id)
        scheduler.cancelReminder(id)
    }

    /** Used by the UI to decide whether to prompt for the exact-alarm permission (Android 12+). */
    suspend fun checkExactAlarmPermission(): Boolean = scheduler.hasExactAlarmPermission()
}

private fun ReminderRow.toDomain(): Reminder = Reminder(
    id = id,
    medicineName = medicine_name,
    dosage = dosage,
    hour = hour.toInt(),
    minute = minute.toInt(),
    isActive = is_active == 1L,
)

// created_at is irrelevant to the scheduler (it only reads id/name/dosage/hour/minute); 0L is a
// safe placeholder on the update path where the stored created_at is left untouched.
private fun Reminder.toRow(): ReminderRow = ReminderRow(
    id = id,
    medicine_name = medicineName,
    dosage = dosage,
    hour = hour.toLong(),
    minute = minute.toLong(),
    is_active = if (isActive) 1L else 0L,
    created_at = 0L,
)
