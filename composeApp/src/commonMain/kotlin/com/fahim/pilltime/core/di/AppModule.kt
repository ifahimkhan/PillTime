package com.fahim.pilltime.core.di

import com.fahim.pilltime.data.local.DatabaseDriverFactory
import com.fahim.pilltime.data.repository.ReminderRepository
import com.fahim.pilltime.db.PillTimeDatabase
import com.fahim.pilltime.presentation.addedit.AddEditReminderViewModel
import com.fahim.pilltime.presentation.reminderlist.ReminderListViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Platform-specific bindings: [DatabaseDriverFactory] and the
 * [com.fahim.pilltime.core.notification.ReminderScheduler] actual. Implemented in androidMain/iosMain.
 */
expect val platformModule: Module

/**
 * Common bindings shared by both platforms. The database is built from the platform driver factory;
 * ViewModels are `factory` scoped so each screen gets a fresh instance. AddEdit takes an optional
 * reminder id parameter (null = add, non-null = edit) via Koin `parametersOf`.
 */
val sharedModule: Module = module {
    single { PillTimeDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { ReminderRepository(get(), get()) }
    factory { ReminderListViewModel(get()) }
    factory { params -> AddEditReminderViewModel(get(), params.getOrNull<Long>()) }
}

/** Single entry point for starting Koin on both platforms. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(sharedModule, platformModule)
}
