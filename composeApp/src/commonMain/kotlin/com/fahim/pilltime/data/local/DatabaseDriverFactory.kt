package com.fahim.pilltime.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific factory for the SQLDelight [SqlDriver]. Android needs an Android context;
 * iOS uses the native driver. Constructed by the platform Koin module, consumed only when building
 * the single [com.fahim.pilltime.db.PillTimeDatabase].
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/** Shared on-disk database file name, kept identical across platforms. */
internal const val DATABASE_NAME = "pilltime.db"
