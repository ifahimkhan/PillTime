package com.fahim.pilltime.core.di

import com.fahim.pilltime.core.notification.ReminderScheduler
import com.fahim.pilltime.data.local.DatabaseDriverFactory
import com.fahim.pilltime.notification.IosReminderScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory() }
    single<ReminderScheduler> { IosReminderScheduler() }
}

private var koinStarted = false

/** Starts Koin once. Safe (and idempotent) to call from Swift startup or [MainViewController]. */
fun doInitKoin() {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
}
