package com.fahim.pilltime.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.fahim.pilltime.db.PillTimeDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(PillTimeDatabase.Schema, DATABASE_NAME)
}
