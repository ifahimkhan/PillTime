package com.fahim.pilltime.domain.model

/**
 * UI/domain representation of a medication reminder. Distinct from the generated SQLDelight row
 * (`com.fahim.pilltime.db.Reminder`): uses `Int` time fields and a `Boolean` active flag, and hides
 * storage-only columns like `created_at`. Mapping lives in `ReminderRepository`.
 */
data class Reminder(
    val id: Long,
    val medicineName: String,
    val dosage: String?,
    val hour: Int,
    val minute: Int,
    val isActive: Boolean,
)
