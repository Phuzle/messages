package com.phuzle.labs.messages.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.ui.components.AppLogo
import com.phuzle.labs.messages.ui.components.IconBadge
import com.phuzle.labs.messages.ui.components.PrimaryButton
import com.phuzle.labs.messages.ui.theme.MessagesTheme
import com.phuzle.labs.messages.ui.theme.ThemeTokens

/**
 * Shown once, before the default-SMS-app role request or any SMS-related runtime permission
 * prompt ever fires — Play Store policy requires an in-app explanation distinct from the system
 * prompt for apps requesting SMS/Call Log access ("Prominent Disclosure"), not just the OS dialog
 * itself. Gated behind AppSettings.smsDisclosureAcknowledged (see AppViewModel.acknowledgeSmsDisclosure).
 *
 * See DESIGN.md's "Startup/status screens" section: header (logo/title/subtitle) centered, the
 * permission list left-aligned with a horizontal icon-badge + title/detail row per item (not
 * icon-above-title-above-detail — that alone is what used to make three short explanations take
 * the entire screen), and the primary action pinned to the bottom of the screen rather than
 * following immediately after the text — the scrollable body and the fixed action are two
 * distinct regions, like any standard permission/onboarding screen.
 */
@Composable
fun SmsDisclosureScreen(onContinue: () -> Unit) {
    val tokens = MessagesTheme.tokens

    Box(Modifier.fillMaxSize().background(tokens.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 56.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo(size = 56.dp)
            Text(
                "Before you continue",
                color = tokens.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "Messages needs a few permissions to work as your SMS app",
                color = tokens.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            Column(
                modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp),
            )
        }

        // A fixed action region at the bottom, not just whatever follows the last line of text —
        // see DESIGN.md. A soft scrim behind it keeps scrolled content from looking like it runs
        // straight under the button with no separation.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(tokens.bg)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            PrimaryButton(label = "Continue", onClick = onContinue)
        }
    }
}

@Composable
private fun DisclosureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    tokens: ThemeTokens,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBadge(icon = icon, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = tokens.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
