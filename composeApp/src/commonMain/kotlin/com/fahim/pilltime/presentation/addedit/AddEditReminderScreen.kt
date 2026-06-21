package com.fahim.pilltime.presentation.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.pilltime.core.ui.CapsuleShape
import com.fahim.pilltime.core.ui.PillTimeColors
import com.fahim.pilltime.core.ui.PillTimeTheme
import com.fahim.pilltime.core.ui.formatTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddEditReminderScreen(
    reminderId: Long?,
    onDone: () -> Unit,
    viewModel: AddEditReminderViewModel = koinViewModel { parametersOf(reminderId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate back once the save completes.
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    AddEditReminderContent(
        state = state,
        onMedicineNameChange = viewModel::onMedicineNameChange,
        onDosageChange = viewModel::onDosageChange,
        onTimeChange = viewModel::onTimeChange,
        onSave = viewModel::save,
        onBack = onDone,
    )
}

/** Stateless content — driven purely by [state] + callbacks, so it is fully previewable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderContent(
    state: AddEditReminderUiState,
    onMedicineNameChange: (String) -> Unit,
    onDosageChange: (String) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "Edit Reminder" else "Add Reminder",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = "Back" },
                    ) { Text("‹", fontSize = 28.sp, color = PillTimeColors.ink) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PillTimeColors.background,
                    titleContentColor = PillTimeColors.ink,
                    navigationIconContentColor = PillTimeColors.ink,
                ),
            )
        },
        containerColor = PillTimeColors.background,
        // The Scaffold handles the status bar at the top; the bottom navigation-bar inset is
        // applied to the scrollable content below so the Save button always clears the nav bar.
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        // Wait for an edit's existing values before building the form, so the time picker is
        // created once with the correct initial time (DB read is near-instant).
        if (!state.isInitialized) return@Scaffold

        // Created once with the loaded values; thereafter the single source of truth for time.
        val timeState = rememberTimePickerState(
            initialHour = state.hour,
            initialMinute = state.minute,
            is24Hour = false,
        )
        // One-directional: push picker edits into the ViewModel (TimePickerState has no setter).
        LaunchedEffect(timeState.hour, timeState.minute) {
            onTimeChange(timeState.hour, timeState.minute)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PillTimeColors.primary,
                focusedLabelColor = PillTimeColors.primary,
                cursorColor = PillTimeColors.primary,
                unfocusedBorderColor = PillTimeColors.border,
            )

            OutlinedTextField(
                value = state.medicineName,
                onValueChange = onMedicineNameChange,
                label = { Text("Medicine name") },
                singleLine = true,
                shape = CapsuleShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.dosage,
                onValueChange = onDosageChange,
                label = { Text("Dosage") },
                placeholder = { Text("e.g. 1 tablet, 10mg") },
                singleLine = true,
                shape = CapsuleShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Remind me at", fontWeight = FontWeight.SemiBold, color = PillTimeColors.ink)
            Text(
                formatTime(state.hour, state.minute),
                fontFamily = FontFamily.Monospace,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = PillTimeColors.ink,
            )
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timeState)
            }

            Button(
                onClick = onSave,
                enabled = state.canSave,
                shape = CapsuleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PillTimeColors.primary,
                    contentColor = Color.White,
                    disabledContainerColor = PillTimeColors.border,
                    disabledContentColor = PillTimeColors.inkMuted,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(
                    if (state.isEditing) "Save changes" else "Add reminder",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun AddEditReminderPreview() {
    PillTimeTheme {
        AddEditReminderContent(
            state = AddEditReminderUiState(
                medicineName = "Lisinopril",
                dosage = "10mg",
                hour = 8,
                minute = 0,
                isEditing = false,
                isInitialized = true,
            ),
            onMedicineNameChange = {}, onDosageChange = {}, onTimeChange = { _, _ -> },
            onSave = {}, onBack = {},
        )
    }
}
