package com.fahim.pilltime.presentation.reminderlist

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.pilltime.core.platform.openExactAlarmSettings
import com.fahim.pilltime.core.ui.CapsuleShape
import com.fahim.pilltime.core.ui.PillTimeColors
import com.fahim.pilltime.core.ui.PillTimeTheme
import com.fahim.pilltime.core.ui.formatTime
import com.fahim.pilltime.domain.model.Reminder
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReminderListScreen(
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    viewModel: ReminderListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReminderListContent(
        state = state,
        onAddReminder = onAddReminder,
        onEditReminder = onEditReminder,
        onToggle = viewModel::toggleActive,
        onDelete = viewModel::deleteReminder,
        onOpenSettings = ::openExactAlarmSettings,
    )
}

/** Stateless content — driven purely by [state] + callbacks, so it is fully previewable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListContent(
    state: ReminderListUiState,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PillTime", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PillTimeColors.background,
                    titleContentColor = PillTimeColors.ink,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminder,
                shape = CapsuleShape,
                containerColor = PillTimeColors.primary,
                contentColor = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .clearAndSetSemantics { contentDescription = "Add reminder" },
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Medium)
            }
        },
        containerColor = PillTimeColors.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ReminderListUiState.Loading -> Unit // local DB read is near-instant
                is ReminderListUiState.Empty -> EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    onAddReminder = onAddReminder,
                )
                is ReminderListUiState.Loaded -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!s.exactAlarmPermissionGranted) {
                        item { PermissionBanner(onOpenSettings = onOpenSettings) }
                    }
                    items(s.reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggle = { onToggle(reminder.id) },
                            onDelete = { onDelete(reminder.id) },
                            onClick = { onEditReminder(reminder.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    shape: Shape = CapsuleShape,
) {
    val contentColor = if (reminder.isActive) PillTimeColors.ink else PillTimeColors.inkMuted
    Surface(
        onClick = onClick,
        shape = shape,
        color = PillTimeColors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PillTimeColors.border, shape),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTime(reminder.hour, reminder.minute),
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    reminder.medicineName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = contentColor,
                )
                if (!reminder.dosage.isNullOrBlank()) {
                    Text(reminder.dosage, fontSize = 13.sp, color = PillTimeColors.inkMuted)
                }
            }
            Switch(
                checked = reminder.isActive,
                onCheckedChange = { onToggle() },
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription =
                        if (reminder.isActive) "Pause ${reminder.medicineName}" else "Activate ${reminder.medicineName}"
                },
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .clearAndSetSemantics { contentDescription = "Delete ${reminder.medicineName}" },
            ) {
                // "✕" glyph avoids pulling in the material-icons artifact.
                Text("✕", fontSize = 18.sp, color = PillTimeColors.inkMuted)
            }
        }
    }
}

@Composable
private fun PermissionBanner(onOpenSettings: () -> Unit) {
    Surface(
        shape = CapsuleShape,
        color = PillTimeColors.dueSoft,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Without this permission, reminders may arrive late.",
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = PillTimeColors.ink,
            )
            TextButton(onClick = onOpenSettings) {
                Text("Open settings", color = PillTimeColors.due, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onAddReminder: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Empty capsule outline.
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 44.dp)
                .border(2.dp, PillTimeColors.inkMuted, RoundedCornerShape(percent = 50)),
        )
        Spacer(Modifier.size(20.dp))
        Text(
            "No reminders yet.\nAdd the first one to get started.",
            color = PillTimeColors.inkMuted,
            fontSize = 16.sp,
        )
        Spacer(Modifier.size(24.dp))
        androidx.compose.material3.Button(
            onClick = onAddReminder,
            shape = CapsuleShape,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = PillTimeColors.primary,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ),
        ) {
            Text("Add your first reminder", fontWeight = FontWeight.SemiBold)
        }
    }
}

private val sampleReminders = listOf(
    Reminder(1, "Lisinopril", "10mg, 1 tablet", 8, 0, isActive = true),
    Reminder(2, "Metformin", "500mg", 13, 30, isActive = true),
    Reminder(3, "Atorvastatin", null, 21, 15, isActive = false),
)

@Preview
@Composable
private fun ReminderListLoadedPreview() {
    PillTimeTheme {
        ReminderListContent(
            state = ReminderListUiState.Loaded(sampleReminders, exactAlarmPermissionGranted = false),
            onAddReminder = {}, onEditReminder = {}, onToggle = {}, onDelete = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun ReminderListEmptyPreview() {
    PillTimeTheme {
        ReminderListContent(
            state = ReminderListUiState.Empty,
            onAddReminder = {}, onEditReminder = {}, onToggle = {}, onDelete = {}, onOpenSettings = {},
        )
    }
}
