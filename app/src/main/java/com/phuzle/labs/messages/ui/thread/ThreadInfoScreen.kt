package com.phuzle.labs.messages.ui.thread

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.AppViewModel
import com.phuzle.labs.messages.ui.components.AvatarBubble
import com.phuzle.labs.messages.ui.components.BarInset
import com.phuzle.labs.messages.ui.components.GlassBar
import com.phuzle.labs.messages.ui.components.SettingsRowDivider
import com.phuzle.labs.messages.ui.components.roundClickable
import com.phuzle.labs.messages.ui.components.topBarContentPadding
import com.phuzle.labs.messages.ui.model.AppUiState
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

private const val WHATSAPP_PACKAGE = "com.whatsapp"

@Composable
fun ThreadInfoScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val thread = state.currentThread ?: return
    val context = LocalContext.current

    val hasWhatsApp = remember(thread.sender) {
        runCatching { context.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0) }.isSuccess
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(80.dp), start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarBubble(thread.initials, thread.avatarColor, thread.isBusiness, size = 76.dp, photoUri = thread.photoUri)
            Text(thread.displayName, color = tokens.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text("${thread.kindLabel} · ${thread.category.label}", color = tokens.textTertiary, fontSize = 13.sp)

            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(
                    icon = Icons.Filled.Call,
                    label = "Call",
                    modifier = Modifier.weight(1f),
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${thread.sender}"))) },
                )
                if (hasWhatsApp) {
                    ActionButton(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        label = "WhatsApp",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val digits = thread.sender.filter { it.isDigit() || it == '+' }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
                        },
                    )
                }
                ActionButton(
                    icon = Icons.Filled.Archive,
                    label = "Archive",
                    modifier = Modifier.weight(1f),
                    onClick = viewModel::archiveCurrentThread,
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(top = 20.dp).background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium),
            ) {
                InfoRow("Number", thread.sender)
                SettingsRowDivider()
                InfoRow("First contact", thread.firstContactLabel ?: "—")
                SettingsRowDivider()
                InfoRow("Category", thread.category.label)
                SettingsRowDivider()
                InfoRow("Notification channel", thread.channelName)
                SettingsRowDivider()
                InfoRow("Type", thread.kindLabel)
            }

            TextActionRow(
                icon = Icons.Filled.DeleteSweep,
                label = "Clear conversation",
                color = tokens.danger,
                onClick = viewModel::clearCurrentConversation,
            )
            TextActionRow(
                icon = if (thread.isBlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                label = if (thread.isBlocked) "Unblock ${thread.displayName}" else "Block ${thread.displayName}",
                color = if (thread.isBlocked) tokens.accent else tokens.danger,
                onClick = viewModel::toggleBlockCurrent,
            )
        }

        GlassBar(modifier = Modifier.align(Alignment.TopCenter), height = 56.dp, inset = BarInset.Top) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).roundClickable(onClick = viewModel::goBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                }
                Text(thread.infoTitle, color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Column(
        modifier = modifier
            .background(tokens.surface, ShapeMedium)
            .border(1.dp, tokens.border, ShapeMedium)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tokens.accent, modifier = Modifier.size(20.dp))
        Text(label, color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun TextActionRow(icon: ImageVector, label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, tokens.border, ShapeMedium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val tokens = MessagesTheme.tokens
    Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = tokens.textSecondary, fontSize = 13.sp)
        Text(value, color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
