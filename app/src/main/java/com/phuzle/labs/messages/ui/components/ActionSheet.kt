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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.model.ActionSheetUi
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** The bottom sheet triggered by a long-press on a thread row: read/archive/private/delete. */
@Composable
fun ActionSheet(
    sheet: ActionSheetUi?,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
    onArchive: () -> Unit,
    onTogglePrivate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sheet == null) return
    val tokens = MessagesTheme.tokens
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(tokens.overlayBg).clickable(onClick = onDismiss))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(tokens.surface, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(bottom = 20.dp),
        ) {
            Text(
                sheet.sender,
                color = tokens.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(18.dp),
            )
            SettingsRowDivider()
            SheetAction(sheet.markReadLabel, tokens.textPrimary, if (sheet.markReadLabel == "Mark as read") Icons.Filled.MarkEmailRead else Icons.Filled.MarkEmailUnread, onMarkRead)
            SheetAction("Archive", tokens.textPrimary, Icons.Filled.Archive, onArchive)
            SheetAction(sheet.privateLabel, tokens.textPrimary, if (sheet.privateLabel == "Move to Private") Icons.Filled.Lock else Icons.Filled.LockOpen, onTogglePrivate)
            SheetAction("Delete", tokens.danger, Icons.Filled.Delete, onDelete)
        }
    }
}

@Composable
private fun SheetAction(label: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(label, color = color, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp))
    }
}
