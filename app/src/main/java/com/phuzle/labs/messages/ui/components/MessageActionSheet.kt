package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.IconSizes
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** The bottom sheet triggered by a long-press on a message bubble: copy/reply/forward/share/select/delete.
 * Same icon-plus-label row pattern as the dashboard thread-row long-press (see ActionSheet). */
@Composable
fun MessageActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSelect: () -> Unit,
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

            MessageActionRow("Copy", tokens.textPrimary, Icons.Filled.ContentCopy, onCopy)
            SettingsRowDivider()
            MessageActionRow("Reply", tokens.textPrimary, Icons.AutoMirrored.Filled.Reply, onReply)
            SettingsRowDivider()
            MessageActionRow("Forward", tokens.textPrimary, Icons.AutoMirrored.Filled.Send, onForward)
            SettingsRowDivider()
            MessageActionRow("Share", tokens.textPrimary, Icons.Filled.Share, onShare)
            SettingsRowDivider()
            MessageActionRow("Select", tokens.textPrimary, Icons.Outlined.CheckCircleOutline, onSelect)
            SettingsRowDivider()
            MessageActionRow("Delete", tokens.danger, Icons.Filled.Delete, onDelete)
        }
    }
}

@Composable
private fun MessageActionRow(label: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(IconSizes.Medium))
        Text(label, color = color, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp))
    }
}
