package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** Long-press sheet for a message still waiting to send — deliberately just Edit/Delete, not the
 * full Copy/Reply/Forward/Delete set MessageActionSheet offers for real messages: a scheduled
 * message hasn't gone out yet, so "reply" or "forward" don't mean anything for it, and "copy"
 * duplicates what editing the body already lets you do. Shared by the thread view's scheduled
 * bubbles and the Scheduled Messages hub — same sheet, same two actions, regardless of which
 * screen triggered it. */
@Composable
fun ScheduledMessageActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val tokens = MessagesTheme.tokens
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(tokens.overlayBg).clickable(onClick = onDismiss))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(tokens.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(tokens.textTertiary.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
            )

            ScheduledMessageActionRow("Edit", tokens.textPrimary, onEdit)
            SettingsRowDivider()
            ScheduledMessageActionRow("Delete", tokens.danger, onDelete)
        }
    }
}

@Composable
private fun ScheduledMessageActionRow(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label,
        color = color,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 24.dp),
    )
}
