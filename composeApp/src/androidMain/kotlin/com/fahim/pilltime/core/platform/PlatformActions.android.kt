package com.fahim.pilltime.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import org.koin.mp.KoinPlatform

actual fun openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context: Context = KoinPlatform.getKoin().get()
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

actual fun hasNotificationPermission(): Boolean {
    val context: Context = KoinPlatform.getKoin().get()
    // Covers both the API 33+ runtime POST_NOTIFICATIONS grant and the user disabling
    // notifications from system settings on any version.
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

actual fun openNotificationSettings() {
    val context: Context = KoinPlatform.getKoin().get()
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        // API 24–25: no per-app notification screen; fall back to the app details page.
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
