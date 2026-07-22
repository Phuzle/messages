package com.phuzle.labs.messages.ui.thread

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun ThreadInfoScreen(state: AppUiState, viewModel: AppViewModel) {
    val tokens = MessagesTheme.tokens
    val thread = state.currentThread ?: return

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topBarContentPadding(80.dp), start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarBubble(thread.initials, thread.avatarColor, thread.isBusiness, size = 76.dp)
            Text(thread.displayName, color = tokens.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text("${thread.kindLabel} · ${thread.category.label}", color = tokens.textTertiary, fontSize = 13.sp)

            Column(
                Modifier.fillMaxWidth().padding(top = 20.dp).background(tokens.surface, ShapeMedium).border(1.dp, tokens.border, ShapeMedium),
            ) {
                InfoRow("Category", thread.category.label)
                SettingsRowDivider()
                InfoRow("Notification channel", thread.channelName)
                SettingsRowDivider()
                InfoRow("Type", thread.kindLabel)
            }

            val blockColor = if (thread.isBlocked) tokens.accent else tokens.danger
            Text(
                if (thread.isBlocked) "Unblock" else "Block",
                color = blockColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .border(1.dp, tokens.border, ShapeMedium)
                    .clickable(onClick = viewModel::toggleBlockCurrent)
                    .padding(12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
private fun InfoRow(label: String, value: String) {
    val tokens = MessagesTheme.tokens
    Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = tokens.textSecondary, fontSize = 13.sp)
        Text(value, color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
