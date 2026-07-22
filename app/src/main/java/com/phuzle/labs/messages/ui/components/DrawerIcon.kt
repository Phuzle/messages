package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class DrawerIconType { Inbox, Archived, Passbook, Reminders, Settings, RecycleBin, AboutUs, Share }

private fun DrawerIconType.imageVector(): ImageVector = when (this) {
    DrawerIconType.Inbox -> Icons.Filled.Inbox
    DrawerIconType.Archived -> Icons.Filled.Archive
    DrawerIconType.Passbook -> Icons.Filled.AccountBalanceWallet
    DrawerIconType.Reminders -> Icons.Filled.Notifications
    DrawerIconType.Settings -> Icons.Filled.Settings
    DrawerIconType.RecycleBin -> Icons.Filled.Delete
    DrawerIconType.AboutUs -> Icons.Filled.Info
    DrawerIconType.Share -> Icons.Filled.Share
}

/** Nav drawer item icons — a real Material icon per destination, replacing the earlier hand-drawn glyphs. */
@Composable
fun DrawerIcon(type: DrawerIconType, color: Color, modifier: Modifier = Modifier) {
    Icon(imageVector = type.imageVector(), contentDescription = null, tint = color, modifier = modifier.size(20.dp))
}
