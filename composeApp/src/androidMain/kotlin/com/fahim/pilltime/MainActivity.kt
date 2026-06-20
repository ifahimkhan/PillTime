package com.fahim.pilltime

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    // POST_NOTIFICATIONS (Android 13+) is required just to display the reminder notification.
    // The exact-alarm permission is handled separately via the in-app banner (see ReminderListScreen).
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* outcome reflected by the OS */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = android.content.Intent(this, com.fahim.pilltime.notification.AlarmService::class.java)
        stopService(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
