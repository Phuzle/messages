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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** The bottom sheet triggered by a long-press on a message bubble: reply/forward/delete. */
@Composable
fun MessageActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
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
                .background(tokens.surface, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(bottom = 20.dp),
        ) {
            MessageActionRow("Reply", tokens.textPrimary, onReply)
            SettingsRowDivider()
            MessageActionRow("Forward", tokens.textPrimary, onForward)
            SettingsRowDivider()
            MessageActionRow("Delete", tokens.danger, onDelete)
        }
    }
}

@Composable
private fun MessageActionRow(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label,
        color = color,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    )
}
