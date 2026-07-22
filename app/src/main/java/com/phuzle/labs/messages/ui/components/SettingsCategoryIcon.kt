package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.ui.model.SettingsSub
import com.phuzle.labs.messages.ui.theme.MessagesTheme

private fun SettingsSub.imageVector(): ImageVector = when (this) {
    SettingsSub.Notifications -> Icons.Filled.Notifications
    SettingsSub.Appearance -> Icons.Filled.Palette
    SettingsSub.Privacy -> Icons.Filled.Lock
    SettingsSub.Chats -> Icons.AutoMirrored.Filled.Chat
    SettingsSub.Backup -> Icons.Filled.Backup
    SettingsSub.Storage -> Icons.Filled.Storage
    SettingsSub.About -> Icons.Filled.Info
}

/** Settings home row icons — a real Material icon per section, in the design's flat rounded-square chip. */
@Composable
fun SettingsCategoryIcon(section: SettingsSub, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Box(
        modifier = modifier
            .size(36.dp)
            .background(tokens.inputBg, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = section.imageVector(), contentDescription = null, tint = tokens.textSecondary, modifier = Modifier.size(18.dp))
    }
}
