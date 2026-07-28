package com.phuzle.labs.messages.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.AvatarBubble
import com.phuzle.labs.messages.ui.components.BarInset
import com.phuzle.labs.messages.ui.components.FlatTextField
import com.phuzle.labs.messages.ui.components.GlassBar
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.ui.format.formatScheduleTime
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.model.ContactSuggestionUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapePill
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }

    val canSend = state.composeBody.isNotBlank() && (state.composeRecipients.isNotEmpty() || state.composeTo.isNotBlank())

    // Autofocus "To" the moment this screen opens, same as tapping into it by hand would —
    // composing a new message always starts with picking a recipient, so make that the very
    // first thing the keyboard is ready for instead of an extra required tap.
    val toFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        toFocusRequester.requestFocus()
        keyboardController?.show()
    }
    val bodyFocusRequester = remember { FocusRequester() }
    val bodyScrollState = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().imePadding()
                .padding(top = topBarContentPadding(70.dp), start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            if (state.composeRecipients.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    state.composeRecipients.forEach { recipient ->
                        RecipientChip(recipient = recipient, onRemove = { viewModel.removeComposeRecipient(recipient.number) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FlatTextField(
                    value = state.composeTo,
                    onValueChange = viewModel::onComposeToChange,
                    placeholder = "To: name or number",
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    focusRequester = toFocusRequester,
                )
                if (state.composeTo.isNotBlank()) {
                    Box(
                        Modifier.size(30.dp).roundClickable(onClick = viewModel::addTypedComposeRecipient),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, contentDescription = "Add recipient", tint = tokens.accent, modifier = Modifier.size(20.dp)) }
                }
            }
            if (state.availableSims.size > 1) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    val selectedId = state.composeSelectedSubscriptionId ?: state.availableSims.first().subscriptionId
                    state.availableSims.forEach { sim ->
                        val selected = sim.subscriptionId == selectedId
                        Text(
                            sim.label,
                            color = if (selected) tokens.accentText else tokens.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(ShapePill)
                                .background(if (selected) tokens.accent else tokens.surfaceAlt, ShapePill)
                                .clickable(onClick = { viewModel.selectComposeSim(sim.subscriptionId) })
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            com.phuzle.labs.messages.ui.components.SettingsRowDivider()
            Spacer(Modifier.height(12.dp))
            // Fills essentially the whole remaining screen (not just as tall as a couple of typed
            // lines) so there's no large dead zone below the field that looks tappable but isn't —
            // a tap anywhere in this area, even well below the cursor, focuses the field and opens
            // the keyboard, same as tapping directly on the text would.
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures(onTap = { bodyFocusRequester.requestFocus() }) },
            ) {
                FlatTextField(
                    value = state.composeBody,
                    onValueChange = viewModel::onComposeBodyChange,
                    placeholder = "Type a message",
                    fontSize = 15.sp,
                    singleLine = false,
                    fillHeight = true,
                    scrollState = bodyScrollState,
                    focusRequester = bodyFocusRequester,
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                )
                if (bodyScrollState.maxValue > 0) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    BoxWithConstraints(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(4.dp)) {
                        val trackHeightPx = constraints.maxHeight.toFloat()
                        val totalContentPx = trackHeightPx + bodyScrollState.maxValue
                        val thumbHeightFraction = (trackHeightPx / totalContentPx).coerceIn(0.08f, 1f)
                        val thumbOffsetFraction = if (bodyScrollState.maxValue == 0) 0f else bodyScrollState.value.toFloat() / bodyScrollState.maxValue
                        val offsetDp = with(density) { (trackHeightPx * (1 - thumbHeightFraction) * thumbOffsetFraction).toDp() }
                        Box(
                            Modifier
                                .fillMaxHeight(thumbHeightFraction)
                                .align(Alignment.TopEnd)
                                .padding(top = offsetDp)
                                .background(tokens.border, ShapePill),
                        )
                    }
                }
            }
            if (state.settings.showCharCount) {
                Text(
                    "${state.composeBody.length} characters", color = tokens.textTertiary, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(38.dp).background(tokens.surfaceAlt, ShapePill)
                            .roundClickable(onClick = { showDatePicker = true }),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.CalendarMonth, contentDescription = "Schedule send", tint = tokens.textSecondary, modifier = Modifier.size(19.dp)) }

                    state.composeCustomScheduleMillis?.let { millis ->
                        Row(
                            modifier = Modifier
                                .clip(ShapePill)
                                .background(tokens.accentSoft, ShapePill)
                                .clickable(onClick = { viewModel.setComposeCustomSchedule(null) })
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(formatScheduleTime(millis), color = tokens.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Filled.Close, contentDescription = "Clear schedule", tint = tokens.accentText, modifier = Modifier.size(13.dp))
                        }
                    }
                }

                Text(
                    if (state.composeCustomScheduleMillis != null) "Schedule" else "Send",
                    color = if (canSend) tokens.accentText else tokens.textTertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(ShapePill)
                        .background(if (canSend) tokens.accent else tokens.surfaceAlt, ShapePill)
                        .let { if (canSend) it.clickable(onClick = viewModel::sendCompose) else it }
                        .padding(horizontal = 24.dp, vertical = 11.dp),
                )
            }
        }

        // A small inline dropdown made someone scroll to even see who they might be texting —
        // this takes over essentially the whole screen instead, like a real contact picker, for
        // as long as there's anything to show. Dismisses itself the instant the match list empties
        // (text cleared, a contact picked, or nothing left matching what's typed). Rendered
        // *before* GlassBar below so the top bar's Close button stays on top of it and reachable —
        // this used to be added after GlassBar in the Box, so its opaque background painted over
        // the bar and trapped the user with no way out except picking a suggestion or clearing
        // the field by hand.
        if (state.composeToSuggestions.isNotEmpty()) {
            Column(
                Modifier.fillMaxSize().background(tokens.bg)
                    .padding(top = topBarContentPadding(126.dp)),
            ) {
                Text(
                    "Suggested contacts", color = tokens.textSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.composeToSuggestions, key = { it.number }) { contact ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { viewModel.selectComposeContact(contact) })
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            AvatarBubble(
                                category = com.phuzle.labs.messages.domain.model.Category.Personal, color = tokens.accent, isBusiness = false,
                                size = 44.dp, photoUri = contact.photoUri,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(contact.name, color = tokens.textPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                                Text(contact.number, color = tokens.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        GlassBar(modifier = Modifier.align(Alignment.TopCenter), height = 56.dp, inset = BarInset.Top) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("New message", color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    Modifier.size(36.dp).roundClickable(onClick = viewModel::closeCompose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val zone = ZoneId.systemDefault()
                    val date = Instant.ofEpochMilli(pickedDateMillis ?: System.currentTimeMillis()).atZone(ZoneOffset.UTC).toLocalDate()
                    val dateTime = date.atTime(timePickerState.hour, timePickerState.minute).atZone(zone)
                    viewModel.setComposeCustomSchedule(dateTime.toInstant().toEpochMilli())
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

@Composable
private fun RecipientChip(recipient: ContactSuggestionUi, onRemove: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = Modifier
            .clip(ShapePill)
            .background(tokens.surfaceAlt, ShapePill)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(recipient.name, color = tokens.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        Box(
            Modifier.size(20.dp).roundClickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Close, contentDescription = "Remove", tint = tokens.textTertiary, modifier = Modifier.size(13.dp)) }
    }
}
