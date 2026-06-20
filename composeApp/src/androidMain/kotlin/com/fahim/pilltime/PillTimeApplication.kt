package com.fahim.pilltime

import android.app.Application
import com.fahim.pilltime.core.di.initKoin
import org.koin.android.ext.koin.androidContext

/**
 * Starts Koin once for the whole process. Must run before any component resolves dependencies —
 * notably [com.fahim.pilltime.notification.BootReceiver], which injects the database + scheduler
 * after a reboot.
 */
class PillTimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@PillTimeApplication) }
    }
}
