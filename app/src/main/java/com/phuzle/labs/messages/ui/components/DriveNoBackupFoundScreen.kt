package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/**
 * Startup step shown when a *silent*, zero-interaction sign-in resolved a Google account but that
 * account has no Messages backup (see AppViewModel.checkDriveBackupsAndOffer) — without this the
 * user was never told a check even happened for this branch, nor which account got checked, nor
 * given any chance to try a different one before this one-time startup window closes for good.
 *
 * Layout matches DESIGN.md's "Startup/status screens" pattern shared with the rest of this flow.
 */
@Composable
fun DriveNoBackupFoundScreen(email: String, onSwitchAccount: () -> Unit, onContinue: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Box(Modifier.fillMaxSize().background(tokens.bg)) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 28.dp).padding(top = 120.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(icon = Icons.Filled.CloudOff, size = 56.dp)
            Text(
                "No backup found",
                color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "We checked $email and didn't find a Messages backup there. If your backup is under a different Google account, you can switch and check again.",
                color = tokens.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(tokens.bg)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            PrimaryButton(label = "Switch Google account", onClick = onSwitchAccount)
            SecondaryTextButton(label = "Continue", onClick = onContinue, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
