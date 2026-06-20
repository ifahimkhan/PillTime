package com.fahim.pilltime.core.di

import com.fahim.pilltime.core.notification.ReminderScheduler
import com.fahim.pilltime.data.local.DatabaseDriverFactory
import com.fahim.pilltime.notification.AndroidReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
}
