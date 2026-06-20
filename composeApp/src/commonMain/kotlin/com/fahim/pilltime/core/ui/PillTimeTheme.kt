package com.fahim.pilltime.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * PillTime design tokens from design.md. Clinical-calm palette; `due`/`dueSoft` are functional
 * alert colors (only for "due now" or the exact-alarm permission banner), never decorative.
 */
object PillTimeColors {
    val background = Color(0xFFF7F5F0)
    val surface = Color(0xFFFFFFFF)
    val ink = Color(0xFF1F2A24)
    val inkMuted = Color(0xFF5C6B62)
    val primary = Color(0xFF3C6E5C)
    val primarySoft = Color(0xFFDCE8E2)
    val due = Color(0xFFE0683C)
    val dueSoft = Color(0xFFFBE3D8)
    val border = Color(0xFFE5E1D8)
}

/** Full pill radius for buttons, switches, chips and reminder rows. */
val CapsuleShape = RoundedCornerShape(percent = 50)

private val PillTimeColorScheme = lightColorScheme(
    primary = PillTimeColors.primary,
    onPrimary = Color.White,
    primaryContainer = PillTimeColors.primarySoft,
    onPrimaryContainer = PillTimeColors.ink,
    secondary = PillTimeColors.primary,
    onSecondary = Color.White,
    secondaryContainer = PillTimeColors.primarySoft,
    onSecondaryContainer = PillTimeColors.ink,
    background = PillTimeColors.background,
    onBackground = PillTimeColors.ink,
    surface = PillTimeColors.surface,
    onSurface = PillTimeColors.ink,
    surfaceVariant = PillTimeColors.primarySoft,
    onSurfaceVariant = PillTimeColors.inkMuted,
    outline = PillTimeColors.border,
    outlineVariant = PillTimeColors.border,
    error = PillTimeColors.due,
    onError = Color.White,
    errorContainer = PillTimeColors.dueSoft,
    onErrorContainer = PillTimeColors.ink,
)

@Composable
fun PillTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PillTimeColorScheme, content = content)
}

/** Format a stored hour (0-23) + minute into a 12-hour `h:mm a` label, e.g. "8:05 AM". */
fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h = (hour % 12).let { if (it == 0) 12 else it }
    val m = minute.toString().padStart(2, '0')
    return "$h:$m $period"
}
