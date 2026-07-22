package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/**
 * Full-screen gate shown in place of the main app — mirrors the "not the default SMS app" style
 * blocking screen — while [com.phuzle.labs.messages.core.sms.SmsHistoryImporter] backfills
 * pre-existing on-device SMS the first time we gain the default-SMS-app role.
 */
@Composable
fun SyncingScreen(done: Int, total: Int, modifier: Modifier = Modifier) {
    val tokens = MessagesTheme.tokens
    Box(modifier.fillMaxSize().background(tokens.bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            AppLogo(size = 60.dp)

            if (total > 0) {
                LinearProgressIndicator(
                    progress = { (done.toFloat() / total).coerceIn(0f, 1f) },
                    color = tokens.accent,
                    trackColor = tokens.surfaceAlt,
                    modifier = Modifier.padding(top = 28.dp).width(220.dp),
                )
            } else {
                CircularProgressIndicator(color = tokens.accent, modifier = Modifier.padding(top = 28.dp))
            }

            Text(
                "Syncing your messages",
                color = tokens.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                if (total > 0) "Synced $done of $total messages" else "Preparing…",
                color = tokens.textTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
