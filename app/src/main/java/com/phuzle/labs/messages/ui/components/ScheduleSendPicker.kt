package com.phuzle.labs.messages.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Holds the two-step "pick a date, then a time" flow's transient state — [start] opens it,
 * [ScheduleSendDialogs] renders it. Pulled out once the compose screen and the in-thread reply bar
 * both needed byte-identical "send later" behavior, including prefilling the time picker with the
 * current time rather than its own midnight default (see ScheduleSendDialogs). */
class ScheduleSendState {
    var showDatePicker by mutableStateOf(false)
    var showTimePicker by mutableStateOf(false)
    var pickedDateMillis by mutableStateOf<Long?>(null)

    fun start() {
        showDatePicker = true
    }
}

@Composable
fun rememberScheduleSendState(): ScheduleSendState = remember { ScheduleSendState() }

/** Renders whichever of the two picker dialogs [state] currently has open; calls [onScheduled]
 * with the final chosen instant once the time is set. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSendDialogs(state: ScheduleSendState, onScheduled: (Long) -> Unit) {
    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { state.showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.pickedDateMillis = datePickerState.selectedDateMillis
                    state.showDatePicker = false
                    state.showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { state.showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (state.showTimePicker) {
        // Prefilled with the current time (as of opening this picker), not TimePickerState's own
        // default of midnight — without this every schedule started the picker at 12:00 AM
        // regardless of when it was opened, forcing a scroll through most of the dial just to
        // reach "now-ish".
        val now = remember(state.showTimePicker) { java.time.LocalTime.now() }
        val timePickerState = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { state.showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val zone = ZoneId.systemDefault()
                    val date = Instant.ofEpochMilli(state.pickedDateMillis ?: System.currentTimeMillis()).atZone(ZoneOffset.UTC).toLocalDate()
                    val dateTime = date.atTime(timePickerState.hour, timePickerState.minute).atZone(zone)
                    onScheduled(dateTime.toInstant().toEpochMilli())
                    state.showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { state.showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) },
        )
    }
}
