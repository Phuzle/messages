package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.phuzle.labs.messages.ui.format.formatScheduleTime
import com.phuzle.labs.messages.ui.model.ScheduledMessageEditUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium
import com.phuzle.labs.messages.ui.theme.ShapePill

/** "Edit" from ScheduledMessageActionSheet — body text and send time, the only two things about a
 * still-pending scheduled message worth changing (who it's going to isn't editable here; that
 * would just be a new message). Reuses ScheduleSendDialogs for the time picker, same as Compose
 * and the thread reply bar's own "send later" flow. */
@Composable
fun EditScheduledMessageDialog(
    edit: ScheduledMessageEditUi,
    onBodyChange: (String) -> Unit,
    onTimeChange: (Long) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MessagesTheme.tokens
    val scheduleState = rememberScheduleSendState()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .background(tokens.modalBg, ShapeMedium)
                .padding(20.dp),
        ) {
            Text("Edit scheduled message", color = tokens.modalText, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            FlatTextField(
                value = edit.body,
                onValueChange = onBodyChange,
                placeholder = "Message",
                singleLine = false,
                maxHeight = 140.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                textColor = tokens.modalText,
                placeholderColor = tokens.modalText.copy(alpha = 0.5f),
            )

            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(tokens.inputBg, ShapePill)
                    .roundClickable(onClick = scheduleState::start)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = tokens.textSecondary, modifier = Modifier.size(16.dp))
                Text(
                    formatScheduleTime(edit.scheduledFor),
                    color = tokens.modalText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            // Stacked full-width, primary-then-secondary — the same pattern every other dialog/
            // startup screen in this app uses (see PrimaryButton/SecondaryTextButton), not a
            // side-by-side row: SecondaryTextButton always fills its available width by design,
            // so pairing it with another button in a Row would have them fight over the same space.
            PrimaryButton(label = "Save", onClick = onSave, modifier = Modifier.padding(top = 20.dp))
            SecondaryTextButton(label = "Cancel", onClick = onDismiss)
        }
    }

    ScheduleSendDialogs(scheduleState, onScheduled = onTimeChange)
}
