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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
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

/** A leading icon inside a filled badge, used everywhere a settings/notification row needs one
 * (settings home nav rows, category rows, app-lock/timer/wifi rows...). A bare glyph with no
 * background reads as small and weak no matter how large the icon itself is drawn — the badge is
 * what actually gives it visual presence, the same way AvatarBubble's colored circle does for a
 * category glyph in the inbox. [size] is the badge's own size; the icon inside is always ~64% of
 * it, matching AvatarBubble/IconBadge's icon-to-container ratio instead of a fixed sp value that
 * can end up looking lost in a much bigger box. */
@Composable
fun SettingsIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = MessagesTheme.tokens.accentLabel,
    background: Color = MessagesTheme.tokens.accentSoft,
) {
    Box(
        modifier = modifier.size(size).background(background, RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.64f))
    }
}

/** Settings home row icons — a real Material icon per section, in the design's flat rounded-square chip. */
@Composable
fun SettingsCategoryIcon(section: SettingsSub, modifier: Modifier = Modifier) {
    SettingsIconBadge(icon = section.imageVector(), modifier = modifier)
}
