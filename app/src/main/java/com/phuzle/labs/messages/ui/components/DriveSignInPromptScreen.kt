package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/** Startup step shown when silent Google Sign-In couldn't determine whether a Drive backup exists
 * (see AppViewModel.checkFirstLaunchDriveRestore) — most commonly right after this app's own data
 * was cleared, since that wipes the local session state silent sign-in relies on to skip
 * interaction. Offers the real, interactive sign-in instead of just giving up, still with Skip. */
@Composable
fun DriveSignInPromptScreen(onSignIn: () -> Unit, onSkip: () -> Unit) {
    val tokens = MessagesTheme.tokens
    Box(modifier = Modifier.fillMaxSize().background(tokens.bg).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.CloudQueue, contentDescription = null, tint = tokens.accent, modifier = Modifier.padding(bottom = 12.dp))
            Text("Check Google Drive for a backup?", color = tokens.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "We couldn't tell without asking — sign in to check whether your Google account has a Messages backup to restore.",
                color = tokens.textSecondary,
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            )
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.accentText),
                shape = ShapeMedium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign in to Google", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }
            TextButton(onClick = onSkip) {
                Text("Skip", color = tokens.textTertiary, fontSize = 13.sp)
            }
        }
    }
}
