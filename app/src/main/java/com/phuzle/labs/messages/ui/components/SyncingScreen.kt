package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
 *
 * Layout matches DESIGN.md's "Startup/status screens" pattern (same as DriveSignInPromptScreen) —
 * content anchored a fixed distance below the top, not dead-centered in the screen (a small
 * cluster floating in a sea of blank space reads just as wrong as cramped padding does).
 */
@Composable
fun SyncingScreen(
    done: Int,
    total: Int,
    modifier: Modifier = Modifier,
    title: String = "Syncing your messages",
    subtitle: String? = null,
    /** Brief "done!" beat — see AppViewModel.importHistoryOnce — swaps the spinner for a
     * checkmark instead of the screen just vanishing the instant sync actually finishes. */
    success: Boolean = false,
) {
    val tokens = MessagesTheme.tokens
    Box(modifier.fillMaxSize().background(tokens.bg)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 40.dp).padding(top = 120.dp),
        ) {
            AppLogo(size = 56.dp)

            if (success) {
                Box(
                    Modifier.padding(top = 24.dp).size(40.dp).background(tokens.success, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, contentDescription = null, tint = tokens.accentText, modifier = Modifier.size(22.dp)) }
            } else if (total > 0) {
                LinearProgressIndicator(
                    progress = { (done.toFloat() / total).coerceIn(0f, 1f) },
                    color = tokens.accent,
                    trackColor = tokens.surfaceAlt,
                    modifier = Modifier.padding(top = 24.dp).width(220.dp),
                )
            } else {
                CircularProgressIndicator(color = tokens.accent, modifier = Modifier.padding(top = 24.dp))
            }

            Text(
                if (success) "All synced!" else title,
                color = tokens.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                if (success) {
                    "Your messages are ready."
                } else {
                    subtitle ?: if (total > 0) "Synced $done of $total messages" else "Preparing…"
                },
                color = tokens.textSecondary,
                fontSize = 13.5.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
