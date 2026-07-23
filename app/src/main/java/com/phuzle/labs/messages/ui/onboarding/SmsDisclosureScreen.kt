package com.phuzle.labs.messages.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.components.AppLogo
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ShapeMedium

/**
 * Shown once, before the default-SMS-app role request or any SMS-related runtime permission
 * prompt ever fires — Play Store policy requires an in-app explanation distinct from the system
 * prompt for apps requesting SMS/Call Log access ("Prominent Disclosure"), not just the OS dialog
 * itself. Gated behind AppSettings.smsDisclosureAcknowledged (see AppViewModel.acknowledgeSmsDisclosure).
 */
@Composable
fun SmsDisclosureScreen(onContinue: () -> Unit) {
    val tokens = MessagesTheme.tokens

    Box(Modifier.fillMaxSize().background(tokens.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo(size = 56.dp)
            Text(
                "Before you continue",
                color = tokens.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "Messages needs a few permissions to work as your SMS app",
                color = tokens.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
            )

            Column(
                modifier = Modifier.padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                DisclosureRow(
                    icon = Icons.Filled.Sms,
                    title = "Default SMS app",
                    detail = "To receive, send, and organize your texts, Messages needs to become your device's default SMS app. Android will ask you to confirm this next.",
                    tokens = tokens,
                )
                DisclosureRow(
                    icon = Icons.Filled.Chat,
                    title = "Read and categorize on-device",
                    detail = "Every message is sorted into Personal, OTP, Transactions, or Promotions using rules that run entirely on your phone. Message content is never uploaded anywhere.",
                    tokens = tokens,
                )
                DisclosureRow(
                    icon = Icons.Filled.Lock,
                    title = "Your contacts, locally",
                    detail = "Contact names and photos are looked up on-device to label your chats — nothing is sent off your phone unless you explicitly turn on Google Drive backup in Settings.",
                    tokens = tokens,
                )
            }

            Text(
                "You can review exactly what's stored and change backup settings anytime from Settings > Privacy.",
                color = tokens.textTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 28.dp),
            )

            Text(
                "Continue",
                color = tokens.accentText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .background(tokens.accent, ShapeMedium)
                    .clickable(onClick = onContinue)
                    .padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DisclosureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    tokens: com.phuzle.labs.messages.ui.theme.ThemeTokens,
) {
    Column {
        Box(
            modifier = Modifier
                .background(tokens.accentSoft, CircleShape)
                .padding(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tokens.accent, modifier = Modifier)
        }
        Text(title, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
        Text(detail, color = tokens.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
